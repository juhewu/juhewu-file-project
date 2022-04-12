package org.juhewu.file.core.aspect;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.storage.FileStorage;

/**
 * 上传切面调用链结束回调
 */
public interface UploadAspectChainCallback {

    FileInfo run(FileInfo fileInfo, UploadPretreatment pre, FileStorage fileStorage);
}
