/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.qp.util;

import cn.hutool.core.io.IORuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className StreamUtil
 * @description 文件流读取工具
 * @date 2024/10/9 14:15
 **/
public class StreamUtil {

    private StreamUtil(){

    }

    /**
     * 获取数据内容
     * 例如： /data.txt
     * @param path resource 下的文件路径
     * @return 返回数据集合
     * @since 0.1.67
     */
    public static List<String> readAllLines(final String path) throws IOException {
        InputStream inputStream = StreamUtil.class.getResourceAsStream(path);
        return readAllLines(inputStream, StandardCharsets.UTF_8, true);
    }

    /**
     * 构建数据集合
     * @param is 文件输入流
     * @return 返回数据集合
     * @since 0.1.67
     */
    public static List<String> readAllLines(final InputStream is) throws IOException {
        return readAllLines(is, StandardCharsets.UTF_8, true);
    }

    /**
     * 构建数据集合
     *
     * @param is 文件输入流
     * @param charset 文件编码
     * @param ignoreEmpty 是否忽略空白行
     * @return 返回数据集合
     * @since 0.1.67
     */
    public static List<String> readAllLines(InputStream is,
                                            final Charset charset,
                                            final boolean ignoreEmpty) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader e = new BufferedReader(new InputStreamReader(is, charset));

        while (e.ready()) {
            String entry = e.readLine();
            if (StringUtils.isEmpty(entry)
                    && ignoreEmpty) {
                continue;
            }
            lines.add(entry);
        }
        return lines;
    }

    /**
     * @description 获取resource下资源文件，适用于springboot项目
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/4 14:55
     * @param path 相对路径
     * @return InputStream
     */
    public static InputStream getResourceStream(final String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        return classPathResource.getInputStream();
    }

    /**
     * @description 获取resource文本流
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/4 14:58
     * @param path  相对路径
     * @param charset  编码
     * @return InputStreamReader
     */
    public static InputStreamReader getResourceStreamReader(final String path, final Charset charset) throws IOException {
        return new InputStreamReader(getResourceStream(path), charset);
    }

    /**
     * @description 资源名称
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/4 15:15
     * @param path 相对路径
     * @return String
     */
    public static String getResourceName(final String path) {
        return new ClassPathResource(path).getFilename();
    }

    /**
     * @description 读取流数据，适用于文本流
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/4 15:35
     * @param stream       文本流
     * @param lineHandler  行处理器
     * @return void
     */
    public static void readUtf8Lines(final InputStream stream, Consumer<String> lineHandler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))){
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                lineHandler.accept(line);
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * @description 读取流数据，适用于文本流
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/11/4 15:36
     * @param path          文件路径
     * @param lineHandler   行处理器
     * @return void
     */
    public static void readUtf8Lines(final String path, Consumer<String> lineHandler) throws IOException {
        InputStream resourceStream = getResourceStream(path);
        readUtf8Lines(resourceStream, lineHandler);
    }
}
