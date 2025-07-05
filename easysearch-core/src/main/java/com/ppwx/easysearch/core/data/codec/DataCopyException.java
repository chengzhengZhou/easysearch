package com.ppwx.easysearch.core.data.codec;

/**
 * Created by lubiao on 2017/3/22.
 */
public class DataCopyException extends RuntimeException{
    public DataCopyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DataCopyException() {
    }

    public DataCopyException(String message) {
        super(message);
    }

    public DataCopyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataCopyException(Throwable cause) {
        super(cause);
    }
}
