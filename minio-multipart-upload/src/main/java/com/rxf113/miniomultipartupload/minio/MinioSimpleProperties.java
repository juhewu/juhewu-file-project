package com.rxf113.miniomultipartupload.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author rxf113
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioSimpleProperties {

    private String url;

    private String username;

    private String password;

}
