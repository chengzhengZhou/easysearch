package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className AbstractMetaSourceConvertor
 * @description 基类实现
 * @date 2024/10/11 15:11
 **/
public abstract class AbstractFileSourceConvertor implements MetaSourceConvertor {

    /** The size of the buffer */
    protected static final int BSIZE = 1024 * 1024;

    /**
     * 数据文件
     */
    protected final String inputDataPath;

    public AbstractFileSourceConvertor(String inputDataPath) {
        this.inputDataPath = inputDataPath;
    }

    /**
     * @description get source
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/12 10:47
     * @return File
     */
    protected File getSourceFile() {
        return FileUtil.file(inputDataPath);
    }

    protected InputStream getSourceStream() throws IOException {
        return StreamUtil.getResourceStream(inputDataPath);
    }
}
