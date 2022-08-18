package org.juhewu.file.core.storage.config;

import lombok.Data;

@Data
public class CustomerBaseConfig extends BaseConfig {
    /**
     * 文件存储类路径，会通过反射初始化此对象
     */
    private Class storageClass;
}
