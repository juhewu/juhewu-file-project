package org.juhewu.file.core.storage;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.config.BaseConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * AWS S3 存储
 *
 * @author duanjw
 * @since 2022/04/11
 */
@Slf4j
public class AwsS3FileStorage implements FileStorage {

    private final AwsS3 awsS3Config;
    private S3Client oss;
    private final String bucketName;
    private final String basePath;
    private final String domain;

    public AwsS3FileStorage(AwsS3 awsS3Config) {
        this.awsS3Config = awsS3Config;
        this.bucketName = awsS3Config.getBucketName();
        this.basePath = StrUtil.addSuffixIfNot(awsS3Config.getBasePath(), StrUtil.EMPTY);
        this.domain = StrUtil.addSuffixIfNot(awsS3Config.getDomain(), StrUtil.EMPTY);

        // 初始化 s3 客户端
        initAswS3Client();

        // 自动创建 bucket
        if (awsS3Config.isBucketAutoCreate()) {
            createBucket();
        }
    }

    /**
     * 初始化 asw-s3 客户端
     */
    private void initAswS3Client() {
        String region = awsS3Config.getRegion();
        String endpoint = awsS3Config.getEndpoint();

        AwsCredentialsProviderChain providerChain = AwsCredentialsProviderChain
                .builder()
                .addCredentialsProvider(() -> AwsBasicCredentials.create(awsS3Config.getAccessKey(), awsS3Config.getSecretKey())).build();


        this.oss = S3Client.builder()
                .credentialsProvider(providerChain)
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
                .build();
    }

    /**
     * 如果 bucket 不存在，自动创建
     */
    private void createBucket() {
        try {
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            oss.createBucket(bucketRequest);
        } catch (Exception e) {
            log.debug("自动创建 bucket： {} 失败", bucketName, e);
        }
    }

    @Override
    public String getStorageId() {
        return awsS3Config.getStorageId();
    }

    @Override
    public boolean upload(FileInfo fileInfo, UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        try {
            this.putObject(bucketName, newFileKey, pre.getFileWrapper().getInputStream());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                fileInfo.setThUrl(domain + newThFileKey);
                this.putObject(bucketName, newThFileKey, new ByteArrayInputStream(thumbnailBytes));
            }
            return true;
        } catch (IOException e) {
            oss.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(newFileKey).build());
            throw new FileStorageException("文件上传失败！platform：" + getStorageId() + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }

    /**
     * 上传文件
     *
     * @param bucketName
     * @param objectName
     * @param stream
     */
    @SneakyThrows
    private void putObject(String bucketName, String objectName, InputStream stream) {
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put("author", "Mary Doe");
//        metadata.put("version", "1.0.0.0");

        // 上传
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
//                .metadata(metadata)
                .build();
        oss.putObject(putOb, RequestBody.fromInputStream(stream, stream.available()));
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            oss.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()).build());
        }
        oss.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(getFileKey(fileInfo)).build());
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return false;
//        return oss.doesObjectExist(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(getFileKey(fileInfo))
                .bucket(bucketName)
                .build();
        try (InputStream in = oss.getObject(objectRequest)) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageException("文件下载失败！platform：" + fileInfo, e);
        }
    }

    private String getFileKey(FileInfo fileInfo) {
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename();
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename())
                .bucket(fileInfo.getStorageId())
                .build();
        try (InputStream in = oss.getObject(objectRequest)) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

//    public void test() {
//
//        AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
//                .roleArn("niceflow")
//                .roleSessionName("test")
//                .build();
//
//        AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
//        Credentials myCreds = roleResponse.credentials();
//
//        // Display the time when the temp creds expire.
//        Instant exTime = myCreds.expiration();
//        String tokenInfo = myCreds.sessionToken();
//
//        // Convert the Instant to readable date.
//        DateTimeFormatter formatter =
//                DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
//                        .withLocale( Locale.US)
//                        .withZone( ZoneId.systemDefault() );
//
//        formatter.format( exTime );
//        System.out.println("The token "+tokenInfo + "  expires on " + exTime );
//
//        AwsCredentialsProviderChain providerChain = AwsCredentialsProviderChain
//                .builder()
//                .addCredentialsProvider(() -> AwsSessionCredentials.create(myCreds.accessKeyId(), myCreds.secretAccessKey(), myCreds.sessionToken())).build();
//
//
//        this.oss = S3Client.builder()
//                .credentialsProvider(providerChain)
//                .region(Region.of(awsS3Config.getRegion()))
//                .endpointOverride(URI.create(awsS3Config.getEndpoint()))
//                .forcePathStyle(true)
//                .build();
//
//    }

    /**
     * AWS S3
     */
    @Data
    public static class AwsS3 extends BaseConfig {

        private String accessKey;
        private String secretKey;
        private String region;
        private String endpoint;
        private String bucketName;
        /**
         * true path-style nginx 反向代理和S3默认支持 pathStyle {http://endpoint/bucketname} false
         * supports virtual-hosted-style 阿里云等需要配置为 virtual-hosted-style
         * 模式{http://bucketname.endpoint}
         */
        private boolean pathStyleAccess = true;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * bucket 不存在自动创建
         */
        private boolean bucketAutoCreate = true;
    }
}
