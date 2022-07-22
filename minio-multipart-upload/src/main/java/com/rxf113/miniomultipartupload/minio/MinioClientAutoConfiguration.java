package com.rxf113.miniomultipartupload.minio;

import com.rxf113.miniomultipartupload.extension.ExtensionMinioClient;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author rxf113
 */
@Configuration
public class MinioClientAutoConfiguration {

    @Bean
    @ConditionalOnClass(value = MinioClient.class)
    public MinioClient minioClient(MinioSimpleProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getUrl())
                .credentials(properties.getUsername(), properties.getPassword())
                .build();
    }

    @Bean
    @ConditionalOnClass(value = ExtensionMinioClient.class)
    public ExtensionMinioClient extensionMinioClient(MinioSimpleProperties properties) {
        MinioAsyncClient build = MinioAsyncClient.builder()
                .endpoint(properties.getUrl())
                .credentials(properties.getUsername(), properties.getPassword())
                .build();
        return new ExtensionMinioClient(build);
    }
}
