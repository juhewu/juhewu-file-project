package org.juhewu.file.core.aspect;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.storage.FileStorage;

/**
 * 文件是否存在切面调用链结束回调
 */
public interface ExistsAspectChainCallback {
    boolean run(FileInfo fileInfo, FileStorage fileStorage);
}
