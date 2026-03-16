package com.ppwx.easysearch.qp.data;

import cn.hutool.core.io.FileUtil;
import com.ppwx.easysearch.qp.util.StreamUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 数据分割
 * 文件合并
 */
public class DataSplitter {

    /**
     * 合并文件
     */
    @Test
    public void mergeFiles() throws IOException {
        String outFile = "export/all-pku.txt";
        String dictionary = "D:\\projects\\yanxuan\\文档\\搜推\\词库采集\\训练语料\\20260302\\train";
        List<String> list = FileUtil.listFileNames(dictionary);
        for (String fileName : list) {
            try(InputStream vocabStream = FileUtil.getInputStream(dictionary + "/" + fileName)) {
                List<String> lists = StreamUtil.readAllLines(vocabStream, StandardCharsets.UTF_8, true);
                FileUtil.writeLines(lists, outFile, StandardCharsets.UTF_8, true);
            }
        }
    }

    /**
     * 按比例随机切分数据为训练集和测试集。
     * 先打乱顺序（固定种子可复现），再按 trainRatio 划分。
     *
     * @param sourcePath   源文件路径（每行一条）
     * @param trainOutPath 训练集输出路径
     * @param testOutPath  测试集输出路径
     * @param trainRatio   训练集占比，如 0.8 表示 80% 训练、20% 测试
     * @param seed         随机种子，相同种子得到相同切分
     */
    public static void splitByRatio(String sourcePath, String trainOutPath, String testOutPath,
                                    double trainRatio, long seed) throws IOException {
        List<String> lines;
        try (InputStream in = FileUtil.getInputStream(sourcePath)) {
            lines = StreamUtil.readAllLines(in, StandardCharsets.UTF_8, true);
        }
        if (lines.isEmpty()) {
            FileUtil.writeLines(Collections.emptyList(), trainOutPath, StandardCharsets.UTF_8, false);
            FileUtil.writeLines(Collections.emptyList(), testOutPath, StandardCharsets.UTF_8, false);
            return;
        }
        Collections.shuffle(lines, new Random(seed));
        int splitIdx = (int) (lines.size() * trainRatio);
        if (splitIdx <= 0) {
            splitIdx = 1;
        } else if (splitIdx >= lines.size()) {
            splitIdx = lines.size() - 1;
        }
        List<String> trainLines = lines.subList(0, splitIdx);
        List<String> testLines = lines.subList(splitIdx, lines.size());
        FileUtil.writeLines(trainLines, trainOutPath, StandardCharsets.UTF_8, false);
        FileUtil.writeLines(testLines, testOutPath, StandardCharsets.UTF_8, false);
    }

    /**
     * 切分数据（测试集合和训练集合）：按比例随机切分
     */
    @Test
    public void splitFile() throws IOException {
        String sourcePath = "export/search-pku.txt";
        String trainOut = "export/search-pku-train.txt";
        String testOut = "export/search-pku-test.txt";
        splitByRatio(sourcePath, trainOut, testOut, 0.9, 42L);
    }

}
