package org.juhewu.file.core.aspect;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.storage.FileStorage;

/**
 * 删除切面调用链结束回调
 */
public interface DeleteAspectChainCallback {
    boolean run(FileInfo fileInfo, FileStorage fileStorage);
}
