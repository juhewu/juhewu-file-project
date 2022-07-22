package com.rxf113.miniomultipartupload.minio;

import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author rxf113
 */
public interface MinioClientService {

    CompletableFuture<Map<String, Object>> getUrls(String bucketName, String objName, int chunkNum) throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException;

    boolean aggMultipart(String uploadId);
}
