package org.juhewu.file.core.storage;

import java.io.InputStream;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;

/**
 * 文件存储，实现此接口，上传、下载文件
 *
 * @author duanjw
 * @since 2022/04/08
 */
public interface FileStorage {

    /**
     * 唯一 id
     */
    String getStorageId();

    /**
     * 上传文件
     *
     * @param fileInfo
     * @param uploadPretreatment
     * @return
     */
    boolean upload(FileInfo fileInfo, UploadPretreatment uploadPretreatment);

    /**
     * 删除文件
     */
    boolean delete(FileInfo fileInfo);

    /**
     * 文件是否存在
     */
    boolean exists(FileInfo fileInfo);

    /**
     * 下载文件
     */
    void download(FileInfo fileInfo, Consumer<InputStream> consumer);

    /**
     * 下载缩略图文件
     */
    void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer);
}
