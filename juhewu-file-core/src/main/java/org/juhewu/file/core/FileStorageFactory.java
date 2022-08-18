package org.juhewu.file.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.FileStorage;

import lombok.Data;

/**
 * 文件存储工厂，从这里获取文件存储
 *
 * @author duanjw
 * @since 2022/04/11
 */
@Data
public class FileStorageFactory {

    private Map<String, FileStorage> fileStorageMap = new HashMap();

    public FileStorage getFileStorage(String key) {
        return Optional.of(fileStorageMap.get(key)).orElseThrow(NullPointerException::new);
    }

    public void addFileStorage(String id, FileStorage fileStorage) {
        if (fileStorageMap.containsKey(id)) {
            throw new FileStorageException("文件存储 id 重复");
        }
        fileStorageMap.put(id, fileStorage);
    }
}
