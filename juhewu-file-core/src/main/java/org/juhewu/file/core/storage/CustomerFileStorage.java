package org.juhewu.file.core.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.storage.config.BaseConfig;

import lombok.Data;

public class CustomerFileStorage implements FileStorage{

    private final Customer customer;

    public CustomerFileStorage(BaseConfig baseConfig) {
        this.customer = new Customer();
        customer.setStorageId(baseConfig.getStorageId());
        Map<String, Object> map = baseConfig.getParams();
        customer.setAccessKey( map.get("accessKey").toString());
    }

    @Override
    public String getStorageId() {
        return customer.getStorageId();
    }

    @Override
    public boolean upload(FileInfo fileInfo, UploadPretreatment uploadPretreatment) {
        throw new UnsupportedOperationException("这是示例代码，不支持上传文件");
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        throw new UnsupportedOperationException("这是示例代码，不支持删除");
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        throw new UnsupportedOperationException("这是示例代码，不支持查看文件是否存在");
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        throw new UnsupportedOperationException("这是示例代码，不支持下载文件");
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        throw new UnsupportedOperationException("这是示例代码，不支持下载文缩略图");

    }

    @Data
    public static class Customer extends BaseConfig {
        private String accessKey;
    }
}
