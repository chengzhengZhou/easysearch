package com.ppwx.easysearch.qp.data;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

/**
 * 文本格式，首行为字段名，次行开始都是数据内容
 * 多个数据使用逗号分割
 * @author ext.ahs.zhouchzh1@jd.com
 * @className TextSourceConvertor
 * @description 文本格式的文件数据转换
 * @date 2024/10/11 15:13
 **/
public class TextSourceConvertor extends AbstractFileSourceConvertor {

    private List<String> header;

    private List<List<String>> data;

    public TextSourceConvertor(String inputDataPath) {
        super(inputDataPath);
    }

    @Override
    public void readData() throws IOException {
        File file = getSourceFile();
        FileInputStream fis = new FileInputStream(file);
        FileChannel fileRead = fis.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
        int len;
        String bufferLine = "";
        byte[] bytes = new byte[BSIZE];
        while ((len = fileRead.read(buffer)) != -1) {
            buffer.flip();
            buffer.get(bytes, 0, len);
            bufferLine = bufferLine.concat(new String(bytes, 0, len));
            bufferLine = bufferLine.replaceAll("\r", "\n");
            String[] bufferData = bufferLine.split("(\n)+");
            boolean isComplete = bufferLine.endsWith("\n");
            int loopLength = isComplete ? bufferData.length : bufferData.length - 1;
            for (int i = 0; i < loopLength; i++) {
                // template method
                handleLine(i, bufferData[i]);

            }
            if (!isComplete) {
                bufferLine = bufferData[bufferData.length - 1];
            }
            buffer.clear();
        }

        fileRead.close();
        fis.close();
    }

    protected void handleLine(int i, String line) {
        String[] values = line.trim().split("[ \t,]+");
        List<String> columns;
        if (i == 0) {
            header = Lists.newArrayList(values);
            data = Lists.newLinkedList();
        } else {
            columns = Lists.newArrayList(values.length > header.size() ? Arrays.copyOf(values, header.size()) : values);
            data.add(columns);
        }
    }

    public List<String> getHeader() {
        return header;
    }

    public List<List<String>> getData() {
        return data;
    }
}
