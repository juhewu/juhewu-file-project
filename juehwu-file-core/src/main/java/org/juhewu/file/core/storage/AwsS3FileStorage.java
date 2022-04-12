package org.juhewu.file.core.storage;

import cn.hutool.core.util.StrUtil;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.config.BaseConfig;

/**
 * AWS S3 存储
 *
 * @author duanjw
 * @since 2022/04/11
 */
@Slf4j
public class AwsS3FileStorage implements FileStorage {

    private final AwsS3 awsS3Config;
    private AmazonS3 oss;
    private final String bucketName;
    private final String basePath;
    private final String domain;

    public AwsS3FileStorage(AwsS3 awsS3Config) {
        this.awsS3Config = awsS3Config;
        this.bucketName = awsS3Config.getBucketName();
        this.basePath = StrUtil.addSuffixIfNot(awsS3Config.getBasePath(), File.separator);
        this.domain = StrUtil.addSuffixIfNot(awsS3Config.getDomain(), File.separator);

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
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsS3Config.getAccessKey(), awsS3Config.getSecretKey())))
                .withPathStyleAccessEnabled(awsS3Config.isPathStyleAccess());
        if (StrUtil.isNotBlank(endpoint)) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region));
        } else if (StrUtil.isNotBlank(region)) {
            builder.withRegion(region);
        }
        this.oss = builder.build();
    }

    /**
     * 如果 bucket 不存在，自动创建
     */
    private void createBucket() {
        try {
            if (!oss.doesBucketExistV2(bucketName)) {
                oss.createBucket((bucketName));
            }
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
            oss.deleteObject(bucketName, newFileKey);
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
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(stream.available());
        objectMetadata.setContentType("application/octet-stream");
        // 上传
        oss.putObject(bucketName, objectName, stream, objectMetadata);
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            oss.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        }
        oss.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return oss.doesObjectExist(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {

        S3Object object = oss.getObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageException("文件下载失败！platform：" + fileInfo, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        S3Object object = oss.getObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

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
