/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataPipelineException
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/9 18:50
 * Description:
 */
package com.ppwx.easysearch.core.common;

/**
 *
 *
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/09 18:50
 * @since 1.0.0
 */
public class DataPipelineException extends DataException {

    public DataPipelineException(String message) {
        super(message);
    }

    public DataPipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataPipelineException(Throwable cause) {
        super(cause);
    }
}