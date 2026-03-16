package com.ppwx.easysearch.qp.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 将多个 {@link TextLineSource} 顺序合并为一个逻辑源。
 * {@link #openStream()} 返回的流会依次读取各源的内容，调用方关闭该流即可。
 * 适用于词典、同义词等多文件合并加载场景。
 */
public final class CompositeTextLineSource implements TextLineSource {

    private final List<TextLineSource> sources;

    public CompositeTextLineSource(List<TextLineSource> sources) {
        this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    }

    public CompositeTextLineSource(TextLineSource... sources) {
        this(sources == null ? Collections.emptyList() : Arrays.asList(sources));
    }

    @Override
    public InputStream openStream() throws IOException {
        if (sources.isEmpty()) {
            return new ByteArrayInputStream(new byte[0]);
        }
        List<InputStream> streams = new ArrayList<>(sources.size());
        try {
            for (TextLineSource source : sources) {
                streams.add(source.openStream());
            }
            SequenceInputStream seq = new SequenceInputStream(Collections.enumeration(streams));
            return new CompositeInputStream(seq, streams);
        } catch (IOException e) {
            for (InputStream is : streams) {
                try {
                    is.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
            throw e;
        }
    }

    /**
     * 包装 SequenceInputStream，在 close 时关闭所有子流，避免资源泄漏。
     */
    private static final class CompositeInputStream extends InputStream {
        private final SequenceInputStream delegate;
        private final List<InputStream> opened;

        CompositeInputStream(SequenceInputStream delegate, List<InputStream> opened) {
            this.delegate = delegate;
            this.opened = opened;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            return delegate.read(bytes, off, len);
        }

        @Override
        public void close() throws IOException {
            IOException first = null;
            try {
                delegate.close();
            } catch (IOException e) {
                first = e;
            }
            for (InputStream is : opened) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (first == null) {
                        first = e;
                    }
                }
            }
            if (first != null) {
                throw first;
            }
        }
    }
}
