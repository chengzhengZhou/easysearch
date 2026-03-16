package com.ppwx.easysearch.qp.source;

import com.ppwx.easysearch.qp.util.StreamUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 基于路径的文本行资源：先尝试 classpath，再尝试文件系统路径。
 */
public class PathTextLineSource implements TextLineSource {

    private final String path;

    public PathTextLineSource(String path) {
        this.path = path;
    }

    @Override
    public InputStream openStream() throws IOException {
        try {
            return StreamUtil.getResourceStream(path);
        } catch (IOException e) {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                return Files.newInputStream(filePath);
            }
            throw new FileNotFoundException("Resource not found: " + path);
        }
    }
}
