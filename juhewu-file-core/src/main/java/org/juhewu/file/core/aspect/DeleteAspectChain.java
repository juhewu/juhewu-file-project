package org.juhewu.file.core.aspect;

import java.util.Iterator;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.storage.FileStorage;

import lombok.Getter;
import lombok.Setter;

/**
 * 删除的切面调用链
 */
@Getter
@Setter
public class DeleteAspectChain {

    private DeleteAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public DeleteAspectChain(Iterable<FileStorageAspect> aspects,DeleteAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileInfo fileInfo, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().deleteAround(this,fileInfo,fileStorage);
        } else {
            return callback.run(fileInfo,fileStorage);
        }
    }
}
