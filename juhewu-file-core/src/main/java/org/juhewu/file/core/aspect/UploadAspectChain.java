package org.juhewu.file.core.aspect;

import java.util.Iterator;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.storage.FileStorage;

import lombok.Getter;
import lombok.Setter;

/**
 * 上传的切面调用链
 */
@Getter
@Setter
public class UploadAspectChain {

    private UploadAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public UploadAspectChain(Iterable<FileStorageAspect> aspects, UploadAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(FileInfo fileInfo, UploadPretreatment pre, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().uploadAround(this,fileInfo,pre,fileStorage);
        } else {
            return callback.run(fileInfo,pre,fileStorage);
        }
    }
}
