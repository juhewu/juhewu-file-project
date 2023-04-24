package com.rxf113.miniomultipartupload;

import com.rxf113.miniomultipartupload.extension.ExtensionMinioClient;
import com.rxf113.miniomultipartupload.util.okhttp.OkhttpEnum;
import io.minio.*;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class TestDemo {

    @Test
    void createBucket() {

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint("http://127.0.0.1:9000")
                    .credentials("admin", "admin123")
                    .build();

            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                    .bucket("rxf113")
                    .build();

            minioClient.makeBucket(makeBucketArgs);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void uploadByUrl() throws IOException {
        File file = new File("C:\\Users\\rxf113\\Documents\\Tencent Files\\1131310577\\FileRecv\\MobileFile\\VID_20220122_162524.mp4");
        FileInputStream inputStream = new FileInputStream(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            outputStream.write(buffer, 0, length);
        }
        byte[] bytes = outputStream.toByteArray();
        int halfLen = bytes.length / 2;

        byte[] bytes1 = new byte[halfLen];
        byte[] bytes2 = new byte[bytes.length - halfLen];
        System.arraycopy(bytes, 0, bytes1, 0, bytes1.length);
        System.arraycopy(bytes, halfLen, bytes2, 0, bytes2.length);

        OkHttpClient client = OkhttpEnum.INSTANCE.getClient();

        Request post1 = new Request.Builder()
                .url("http://127.0.0.1:9000/rxf113/VID_20220122_162524.mp4?uploadId=b777deac-a9a9-4c83-a0a8-0640eeccc1f0&partNumber=1&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20220624%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20220624T075513Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=57b88938dec44e0405d1bfa251e88286f4b0cdeb45d8966eddb353855f9dadd7")
                .put(RequestBody.create(bytes1)).build();


        Call call1 = client.newCall(post1);
        Response response1 = call1.execute();
        if (response1.isSuccessful()) {
            //处理response的响应消息
            //Map value = new ObjectMapper().readValue(response1.body().bytes(), Map.class);
            //System.out.println(value);
        } else {
            throw new IOException("Unexpected code1 " + response1);
        }


        Request post2 = new Request.Builder()
                .url("http://127.0.0.1:9000/rxf113/VID_20220122_162524.mp4?uploadId=b777deac-a9a9-4c83-a0a8-0640eeccc1f0&partNumber=2&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20220624%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20220624T075513Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=b6cfffb10a34b260562e43c9b8991a61647f7b4ed73f34653695fe250f89e735")
                .put(RequestBody.create(bytes2)).build();
        Call call2 = client.newCall(post2);
        Response response2 = call2.execute();
        if (response2.isSuccessful()) {
            //处理response的响应消息
            //Map value = new ObjectMapper().readValue(response2.body().bytes(), Map.class);
            //System.out.println(value);
        } else {
            throw new IOException("Unexpected code2 " + response2);
        }
    }

    @Test
    void agg() throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException {
        MinioAsyncClient build = MinioAsyncClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("admin", "admin123")
                .build();
        ExtensionMinioClient extensionMinioClient = new ExtensionMinioClient(build);
        String uploadId = "2488f991-911e-4b0d-931e-e7868df92d93";
        String objName = "nn.mp4";
        //String bucketName, String region, String delimiter, String encodingType, String keyMarker, Integer maxUploads, String prefix, String uploadIdMarker, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams
        CompletableFuture<ListPartsResponse> completableFuture = extensionMinioClient.listPartsAsync("rxf113", null, objName, 1000, null, uploadId, null, null);
        CompletableFuture<Void> cpl = completableFuture.thenAccept(listPartsResponse -> {
            ListPartsResult partsResult = listPartsResponse.result();
            Part[] parts = new Part[partsResult.partList().size()];
            int partNumber = 1;
            for (Part part : partsResult.partList()) {
                parts[partNumber - 1] = part;
                partNumber++;
            }
            try {
                CompletableFuture<ObjectWriteResponse> rxf113 = extensionMinioClient.completeMultipartUploadAsync("rxf113", null, objName, uploadId, parts, null, null);
                rxf113.thenAccept(objectWriteResponse -> {
                    String etag = objectWriteResponse.etag();
                    String versionId = objectWriteResponse.versionId();
                    System.out.println(123 + " " + etag + " " + versionId);
                });
                rxf113.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        cpl.join();

    }

    @Test
    void splitChunk() throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException {


        MinioAsyncClient build = MinioAsyncClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("admin", "admin123")
                .build();
        ExtensionMinioClient extensionMinioClient = new ExtensionMinioClient(build);
        CompletableFuture<CreateMultipartUploadResponse> completableFuture = extensionMinioClient.createMultipartUploadAsync("rxf113", null, "VID_20220122_162524.mp4", null, null);


        //extraQueryParams 固定

        CompletableFuture<Void> voidCompletableFuture = completableFuture.thenAccept(response -> {
            String uploadId = response.result().uploadId();
            System.out.println("uploadId: " + uploadId);
            for (int i = 1; i <= 2; i++) {
                Map<String, String> extraQueryParams = new HashMap<>(2, 1);
                extraQueryParams.put("uploadId", uploadId);
                extraQueryParams.put("partNumber", Integer.toString(i));
                GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                        .bucket("rxf113")
                        .object("VID_20220122_162524.mp4")
                        .extraQueryParams(extraQueryParams)
                        .expiry(1, TimeUnit.DAYS)
                        .method(Method.PUT)
                        .build();

                try {
                    String uploadUrl = extensionMinioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);// 获取URL
                    System.out.println(uploadUrl);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        voidCompletableFuture.join();
    }


    @Test
    void tt() throws IOException {

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.supplyAsync(() -> {
            return "234";
        }).thenAccept(re -> {
            System.out.println(re);
        });
        voidCompletableFuture.join();
        System.out.println(123);
        String builder = "{\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Action\": [\n" +
                "                \"s3:GetBucketLocation\",\n" +
                "                \"s3:ListBucket\"\n" +
                "            ],\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Resource\": \"arn:aws:s3:::" + "rxf113" + "\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"Action\": \"s3:GetObject\",\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Resource\": \"arn:aws:s3:::" + "rxf113" + "/*\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"Version\": \"2012-10-17\"\n" +
                "}\n";
        System.out.println(builder);
    }

}
