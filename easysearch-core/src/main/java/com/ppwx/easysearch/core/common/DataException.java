/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataException
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/9 18:48
 * Description:
 */
package com.ppwx.easysearch.core.common;

/**
 *
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/09 18:48
 * @since 1.0.0
 */
public class DataException extends RuntimeException {

    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataException(Throwable cause) {
        super(cause);
    }
}