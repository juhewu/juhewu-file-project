package org.juhewu.file.config;

import java.util.ArrayList;
import java.util.List;

import org.juhewu.file.core.storage.AwsS3FileStorage;
import org.juhewu.file.core.storage.config.CustomerBaseConfig;
import org.juhewu.file.core.storage.LocalFileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;


import lombok.Data;

@Data
@ConditionalOnMissingBean(FileStorageProperties.class)
@ConfigurationProperties(prefix = "juhewu.file-storage")
public class FileStorageProperties {

    /**
     * 默认存储平台
     */
    private String defaultStorageId = "local";
    /**
     * 缩略图后缀，例如【.min.jpg】【.png】
     */
    private String thumbnailSuffix = ".min.jpg";
    /**
     * 本地存储
     */
    private List<LocalFileStorage.Local> local = new ArrayList<>();

    /**
     * AWS S3
     */
    private List<AwsS3FileStorage.AwsS3> awsS3 = new ArrayList<>();

    private List<CustomerBaseConfig> other = new ArrayList<>();


}
