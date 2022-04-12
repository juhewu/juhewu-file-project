package org.juhewu.file.core.aspect;

import java.io.InputStream;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.storage.FileStorage;

/**
 * 下载切面调用链结束回调
 */
public interface DownloadAspectChainCallback {
    void run(FileInfo fileInfo, FileStorage fileStorage,Consumer<InputStream> consumer);
}
