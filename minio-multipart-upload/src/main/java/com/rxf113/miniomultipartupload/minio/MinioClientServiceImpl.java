package com.rxf113.miniomultipartupload.minio;

import com.rxf113.miniomultipartupload.extension.ExtensionMinioClient;
import com.rxf113.miniomultipartupload.util.CacheUtil;
import io.minio.*;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author rxf113
 */
@Slf4j
@Service
@AllArgsConstructor
public class MinioClientServiceImpl implements MinioClientService {

    private final MinioClient minioClient;

    private final ExtensionMinioClient extensionMinioClient;

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getUrls(String bucketName, String objName, int chunkNum) throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException {
        CompletableFuture<CreateMultipartUploadResponse> multipartUploadCompletableFuture = extensionMinioClient.createMultipartUploadAsync(bucketName, null, objName, null, null);
        return multipartUploadCompletableFuture.thenApply(multipartUploadResponse -> {
            Map<String, Object> resultMap = new HashMap<>(2, 1);
            String uploadId = multipartUploadResponse.result().uploadId();
            resultMap.put("uploadId", uploadId);
            //缓存 uploadId 对应 bucket 和 文件名
            CacheUtil.put(uploadId, new String[]{bucketName, objName});
            List<String> urls = (List<String>) resultMap.computeIfAbsent("urls", key -> new ArrayList<>());
            log.info("开始下发url, bucketName: {} , name: {}, uploadId: [{}]", bucketName, objName, uploadId);

            for (int i = 1; i <= chunkNum; i++) {
                Map<String, String> extraQueryParams = new HashMap<>(2, 1);
                extraQueryParams.put("uploadId", uploadId);
                if (chunkNum != 1) {
                    //分块
                    extraQueryParams.put("partNumber", Integer.toString(i));
                }
                GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objName)
                        .extraQueryParams(extraQueryParams)
                        //todo 暂定10分钟
                        .expiry(10, TimeUnit.MINUTES)
                        .method(Method.PUT)
                        .build();
                try {
                    // 获取URL
                    String uploadUrl = extensionMinioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
                    log.info("下发url成功, uploadId: [{}], url: {}", uploadId, uploadUrl);
                    urls.add(uploadUrl);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return resultMap;
        });
    }

    @SuppressWarnings("all")
    @Override
    public boolean aggMultipart(String uploadId) {
        String[] bucketAndName = (String[]) CacheUtil.get(uploadId);
        Assert.notNull(bucketAndName, "g 数据异常!");
        String bucketName = bucketAndName[0];
        String objName = bucketAndName[1];
        log.info("聚合 part bucketName: {} , name: {}, uploadId: [{}]", bucketAndName, objName, uploadId);
        try {
            CompletableFuture<ListPartsResponse> completableFuture = extensionMinioClient.listPartsAsync(bucketName, null, objName, 255, null, uploadId, null, null);
            return completableFuture.thenApply(listPartsResponse -> {
                ListPartsResult partsResult = listPartsResponse.result();
                Part[] parts = partsResult.partList().toArray(new Part[0]);
                try {
                    CompletableFuture<ObjectWriteResponse> responseCompletableFuture = extensionMinioClient.completeMultipartUploadAsync(bucketName, null, objName, uploadId, parts, null, null);
                    return responseCompletableFuture.thenApply(objectWriteResponse -> {
                        String etag = objectWriteResponse.etag();
                        String versionId = objectWriteResponse.versionId();
                        log.info("聚合完成 versionId: [{}], etag: {}", versionId, etag);
                        return true;
                    }).exceptionally(throwable -> {
                        throw new RuntimeException(throwable);
                    }).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).exceptionally(throwable -> {
                throw new RuntimeException(throwable);
            }).get();
        } catch (Exception e) {
            log.error("聚合失败，part bucketName: {} , name: {}, uploadId: [{}]", bucketAndName, objName, uploadId, e);
            return false;
        }
    }
}
