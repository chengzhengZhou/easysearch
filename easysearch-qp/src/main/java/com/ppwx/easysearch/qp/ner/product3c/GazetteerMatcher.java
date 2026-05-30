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

package com.ppwx.easysearch.qp.ner.product3c;

import com.ppwx.easysearch.qp.source.PathTextLineSource;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 3C 领域词典与参数模式特征构造器。
 */
public class GazetteerMatcher {

    private final Map<Character, Object> brandTrie;
    private final Map<Character, Object> categoryTrie;
    private final Set<String> versionWords;
    private final String dictVersion;

    private static final String GPU_NUMBERS = "1050|1060|1070|1080|1650|1660|2060|2070|2080|3050|3060|3070|3080|3090|"
            + "4050|4060|4070|4080|4090|5050|5060|5070|5080|5090|"
            + "5500|5600|5700|5800|5900|6600|6700|6800|6900|7600|7700|7800|7900|960|970|980";
    private static final Pattern STO = Pattern.compile("^(\\d{1,4})(g|gb|t|tb)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STO_COMBO = Pattern.compile("^\\d{1,3}(g|gb)?\\+\\d{1,4}(g|gb|t|tb)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCR = Pattern.compile("^(\\d+(?:\\.\\d+)?)(英寸|寸|″)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PWR = Pattern.compile("^(\\d+)w$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEN = Pattern.compile("^(\\d+(?:\\.\\d+)?)(mm|毫米)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOCAL = Pattern.compile("\\d{1,4}(?:\\.\\d+)?-\\d{1,4}(?:\\.\\d+)?mm", Pattern.CASE_INSENSITIVE);
    private static final Pattern APERTURE = Pattern.compile("f/?\\d+(?:\\.\\d+)?(?:-\\d+(?:\\.\\d+)?)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOUNT = Pattern.compile("(m4/3|m43|rf|ef|xf|e|f|z|l)卡口", Pattern.CASE_INSENSITIVE);
    private static final Pattern GPU = Pattern.compile("^((geforce|nvidia|quadro)\\s*)?(rtx|gtx|rx)?\\s?(" + GPU_NUMBERS + ")(\\s?(ti|s|super|xt))?([\\w-]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GPU_WITH_PREFIX = Pattern.compile("(rtx|gtx|rx)\\s?(" + GPU_NUMBERS + ")(\\s?(ti|s|super|xt))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CPU = Pattern.compile("^(i\\d|ryzen\\s?\\d|r\\d\\s?\\d{3,5}[a-z]*|m\\d(?:\\smax|\\spro)?|骁龙\\s?\\d+|\\d{4}x3d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CON = Pattern.compile("^(\\d+)新$");
    private static final Pattern YR = Pattern.compile("^(20\\d{2})(年|款)?$");

    public GazetteerMatcher(String dictDir) throws IOException {
        this.brandTrie = buildTrie(readLines(dictDir, "brand.txt"));
        List<String> categories = new ArrayList<>(readLines(dictDir, "category.txt"));
        categories.addAll(readLines(dictDir, "accessory.txt"));
        this.categoryTrie = buildTrie(categories);
        this.versionWords = new HashSet<>(readLines(dictDir, "version.txt"));
        List<String> version = readLines(dictDir, "VERSION");
        this.dictVersion = version.isEmpty() ? "0" : version.get(0);
    }

    private static List<String> readLines(String dictDir, String fileName) throws IOException {
        if (StringUtils.isBlank(dictDir)) {
            return Collections.emptyList();
        }
        String path = dictDir.endsWith("/") ? dictDir + fileName : dictDir + "/" + fileName;
        List<String> lines = new ArrayList<>();
        try (InputStream inputStream = new PathTextLineSource(path).openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            if ("brand.txt".equals(fileName) || "category.txt".equals(fileName)) {
                throw e;
            }
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    private static Map<Character, Object> buildTrie(List<String> words) {
        List<String> sorted = new ArrayList<>(words);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.length(), a.length()));
        Map<Character, Object> root = new HashMap<>();
        for (String word : sorted) {
            Map<Character, Object> node = root;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (!node.containsKey(c)) {
                    node.put(c, new HashMap<Character, Object>());
                }
                node = (Map<Character, Object>) node.get(c);
            }
            node.put('$', word);
        }
        return root;
    }

    public String[][] buildColumns(Product3CTextNormalizer.Result base, Product3CTextNormalizer.SpellingResult spell) {
        String norm = spell.text;
        int n = norm.length();
        String[] c0 = new String[n];
        String[] c1 = new String[n];
        String[] c2 = new String[n];
        String[] c3 = new String[n];
        String[] c4 = new String[n];
        for (int i = 0; i < n; i++) {
            c0[i] = String.valueOf(norm.charAt(i));
            c2[i] = "O";
            c3[i] = "O";
            c4[i] = "O";
            int ci = i < spell.normToCleaned.length ? spell.normToCleaned[i] : Math.min(i, base.charClasses.length - 1);
            c1[i] = String.valueOf(ci >= 0 && ci < base.charClasses.length ? base.charClasses[ci] : Product3CTextNormalizer.charType(norm.charAt(i)));
        }
        markTrie(norm, brandTrie, c2, "B");
        markTrie(norm, categoryTrie, c3, "C");
        markParams(norm, c4);
        return new String[][]{c0, c1, c2, c3, c4};
    }

    @SuppressWarnings("unchecked")
    private void markTrie(String text, Map<Character, Object> trie, String[] col, String prefix) {
        List<int[]> matches = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            Map<Character, Object> node = trie;
            int j = i;
            int last = -1;
            while (j < text.length()) {
                char c = text.charAt(j);
                if (!node.containsKey(c)) {
                    break;
                }
                node = (Map<Character, Object>) node.get(c);
                j++;
                if (node.containsKey('$')) {
                    last = j;
                }
            }
            if (last > i) {
                matches.add(new int[]{i, last});
            }
        }
        for (int[] match : matches) {
            markSpan(col, match[0], match[1], prefix);
        }
    }

    private void markSpan(String[] col, int start, int end, String prefix) {
        for (int i = start; i < end; i++) {
            if (i == start) {
                col[i] = prefix + "-头";
            } else if (i == end - 1) {
                col[i] = prefix + "-尾";
            } else {
                col[i] = prefix + "-中";
            }
        }
    }

    private void markParams(String text, String[] col) {
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == ' ') {
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) != ' ') {
                j++;
            }
            String tag = paramTag(text.substring(i, j));
            if (!"O".equals(tag)) {
                for (int k = i; k < j; k++) {
                    col[k] = tag;
                }
            }
            i = j;
        }
    }

    private String paramTag(String token) {
        String t = token.toLowerCase();
        if (versionWords.contains(t)) {
            return "VER";
        }
        if (STO.matcher(t).matches() || STO_COMBO.matcher(t).matches()) {
            return "STO";
        }
        if (SCR.matcher(t).matches()) {
            return "SCR";
        }
        if (PWR.matcher(t).matches()) {
            return "PWR";
        }
        if (LEN.matcher(t).matches() || FOCAL.matcher(t).find() || APERTURE.matcher(t).find() || MOUNT.matcher(t).find()) {
            return "LEN";
        }
        if (GPU.matcher(t).matches() || GPU_WITH_PREFIX.matcher(t).find()) {
            return "GPU";
        }
        if (YR.matcher(t).matches()) {
            return "YR";
        }
        if (CPU.matcher(t).find()) {
            return "CPU";
        }
        if (CON.matcher(t).matches()) {
            return "CON";
        }
        return "O";
    }

    public String getDictVersion() {
        return dictVersion;
    }
}
