package org.juhewu.file.core.exception;

import org.juhewu.core.base.exception.BusinessException;
import org.juhewu.core.base.exception.ICodeMessage;

public class FileStorageException extends BusinessException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String code, String message) {
        super(code, message);
    }

    public FileStorageException(Throwable cause) {
        super(cause);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FileStorageException(ICodeMessage codeMessage) {
        super(codeMessage);
    }
}
