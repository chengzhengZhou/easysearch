package com.ppwx.easysearch.qp.source;

import java.io.IOException;
import java.io.InputStream;

/**
 * 词典/同义词等文本行资源的统一抽象。
 * 支持文件、classpath、数据库等来源，消费者仅依赖本接口即可加载外部资源。
 */
public interface TextLineSource {

    /**
     * 按 UTF-8 打开输入流，调用方负责关闭。
     *
     * @return 文本输入流，不得为 null
     * @throws IOException 打开失败时抛出
     */
    InputStream openStream() throws IOException;
}
