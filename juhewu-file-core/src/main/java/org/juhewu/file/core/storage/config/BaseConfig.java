package org.juhewu.file.core.storage.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class BaseConfig {

    /**
     * 存储 id，必须唯一
     */
    private String storageId;
    /**
     * 启用存储
     */
    private boolean enable = true;
    /**
     * 其它参数
     */
    private Map<String, Object> params = new HashMap();
}