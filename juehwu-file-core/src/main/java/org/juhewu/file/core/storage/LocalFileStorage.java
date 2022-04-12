package org.juhewu.file.core.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.config.BaseConfig;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 本地文件存储
 *
 * @author duanjw
 * @since 2022/04/08
 */
@AllArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final Local local;

    @Override
    public String getStorageId() {
        return local.getStorageId();
    }

    @Override
    public boolean upload(FileInfo fileInfo, UploadPretreatment uploadPretreatment) {
        String path = fileInfo.getPath();
        /* 本地存储路径*/
        String basePath = StrUtil.addSuffixIfNot(local.getBasePath(), File.separator);
        /* 存储平台 */
        String id = local.getStorageId();
        /* 访问域名 */
        String domain = StrUtil.addSuffixIfNot(local.getDomain(), File.separator);

        File newFile = FileUtil.touch(basePath + path, fileInfo.getFilename());
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + path + fileInfo.getFilename());

        try {
            uploadPretreatment.getFileWrapper().transferTo(newFile);

            byte[] thumbnailBytes = uploadPretreatment.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                fileInfo.setThUrl(domain + path + fileInfo.getThFilename());
                FileUtil.writeBytes(thumbnailBytes, basePath + path + fileInfo.getThFilename());
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(newFile);
            throw new FileStorageException("文件上传失败！id：" + id + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            FileUtil.del(new File(fileInfo.getBasePath() + fileInfo.getPath(), fileInfo.getThFilename()));
        }
        return FileUtil.del(new File(fileInfo.getBasePath() + fileInfo.getPath(), fileInfo.getFilename()));
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return new File(fileInfo.getBasePath() + fileInfo.getPath(), fileInfo.getFilename()).exists();
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename())) {
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
        try (InputStream in = FileUtil.getInputStream(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename())) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    /**
     * 本地存储
     */
    @Data
    public static class Local extends BaseConfig {

        /**
         * 本地存储路径
         */
        private String basePath = "";
        /**
         * 本地存储访问路径
         */
        private String[] pathPatterns = new String[0];
        /**
         * 启用本地访问
         */
        private boolean enableAccess = false;
        /**
         * 访问域名
         */
        private String domain = "";
    }
}
