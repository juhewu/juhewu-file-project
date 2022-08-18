package org.juhewu.file.core.aspect;

import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.storage.FileStorage;

import lombok.Getter;
import lombok.Setter;

/**
 * 下载的切面调用链
 */
@Getter
@Setter
public class DownloadAspectChain {

    private DownloadAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public DownloadAspectChain(Iterable<FileStorageAspect> aspects, DownloadAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public void next(FileInfo fileInfo, FileStorage fileStorage,Consumer<InputStream> consumer) {
        if (aspectIterator.hasNext()) {//还有下一个
            aspectIterator.next().downloadAround(this,fileInfo,fileStorage,consumer);
        } else {
            callback.run(fileInfo,fileStorage,consumer);
        }
    }
}
