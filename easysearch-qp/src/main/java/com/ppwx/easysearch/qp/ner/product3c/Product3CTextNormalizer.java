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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 与训练侧保持一致的 3C NER 文本归一化与拼写归一逻辑。
 */
public final class Product3CTextNormalizer {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final String REPLACEMENT = "\uFFFD";

    private Product3CTextNormalizer() {
    }

    public static class Result {
        public final String cleaned;
        public final char[] charClasses;
        public final int[] cleanedToRawStart;
        public final int[] cleanedToRawEnd;

        Result(String cleaned, char[] charClasses, int[] cleanedToRawStart, int[] cleanedToRawEnd) {
            this.cleaned = cleaned;
            this.charClasses = charClasses;
            this.cleanedToRawStart = cleanedToRawStart;
            this.cleanedToRawEnd = cleanedToRawEnd;
        }
    }

    public static class SpellingResult {
        public final String text;
        public final int[] normToCleaned;
        public final int[] normToRawStart;
        public final int[] normToRawEnd;

        public SpellingResult(String text, int[] normToCleaned, int[] normToRawStart, int[] normToRawEnd) {
            this.text = text;
            this.normToCleaned = normToCleaned;
            this.normToRawStart = normToRawStart;
            this.normToRawEnd = normToRawEnd;
        }
    }

    public static Result normalize(String raw) {
        if (raw == null) {
            raw = "";
        }
        StringBuilder expanded = new StringBuilder();
        List<Integer> rawStarts = new ArrayList<>();
        List<Integer> rawEnds = new ArrayList<>();
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int rawEnd = i + Character.charCount(cp);
            if (cp != REPLACEMENT.codePointAt(0)) {
                String normalized = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFKC);
                for (int j = 0; j < normalized.length(); j++) {
                    char ch = normalized.charAt(j);
                    if (isAllowed(ch)) {
                        expanded.append(ch);
                    } else {
                        expanded.append(' ');
                    }
                    rawStarts.add(i);
                    rawEnds.add(rawEnd);
                }
            }
            i = rawEnd;
        }
        return collapseSpaces(expanded, rawStarts, rawEnds);
    }

    private static Result collapseSpaces(StringBuilder expanded, List<Integer> rawStarts, List<Integer> rawEnds) {
        String collapsed = MULTI_SPACE.matcher(expanded.toString()).replaceAll(" ").trim();
        StringBuilder cleaned = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        int source = 0;
        while (source < expanded.length() && Character.isWhitespace(expanded.charAt(source))) {
            source++;
        }
        for (int i = 0; i < collapsed.length(); i++) {
            char target = collapsed.charAt(i);
            if (target == ' ') {
                while (source < expanded.length() && !Character.isWhitespace(expanded.charAt(source))) {
                    source++;
                }
                int firstSpace = source;
                while (source < expanded.length() && Character.isWhitespace(expanded.charAt(source))) {
                    source++;
                }
                if (source >= expanded.length()) {
                    break;
                }
                cleaned.append(' ');
                starts.add(rawStarts.get(firstSpace));
                ends.add(rawEnds.get(source - 1));
            } else {
                while (source < expanded.length() && Character.isWhitespace(expanded.charAt(source))) {
                    source++;
                }
                if (source >= expanded.length()) {
                    break;
                }
                cleaned.append(expanded.charAt(source));
                starts.add(rawStarts.get(source));
                ends.add(rawEnds.get(source));
                source++;
            }
        }
        char[] classes = new char[cleaned.length()];
        for (int i = 0; i < cleaned.length(); i++) {
            classes[i] = charType(cleaned.charAt(i));
        }
        return new Result(cleaned.toString(), classes, toIntArray(starts), toIntArray(ends));
    }

    private static boolean isAllowed(char ch) {
        if (ch == ' ' || "+-/().".indexOf(ch) >= 0) {
            return true;
        }
        if (ch >= 0x4E00 && ch <= 0x9FFF) {
            return true;
        }
        return Character.isLetterOrDigit(ch);
    }

    public static char charType(char ch) {
        if (ch == ' ') {
            return 'S';
        }
        if (ch == '-') {
            return '+';
        }
        if (Character.isDigit(ch)) {
            return 'D';
        }
        if (ch >= 0x4E00 && ch <= 0x9FFF) {
            return 'C';
        }
        if (ch >= 'a' && ch <= 'z') {
            return 'L';
        }
        if (ch >= 'A' && ch <= 'Z') {
            return 'U';
        }
        return 'P';
    }

    public static Map<String, String> loadSpellingMap(String path) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isBlank(path)) {
            return map;
        }
        try (InputStream inputStream = new PathTextLineSource(path).openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length >= 2) {
                    map.put(parts[0].toLowerCase(), parts[1].toLowerCase());
                }
            }
        }
        return map;
    }

    public static SpellingResult applySpelling(Result base, Map<String, String> spelling) {
        if (base == null) {
            base = normalize("");
        }
        String cleaned = base.cleaned;
        if (spelling == null) {
            spelling = new HashMap<>();
        }
        StringBuilder out = new StringBuilder();
        List<Integer> normToCleaned = new ArrayList<>();
        List<Integer> normToRawStart = new ArrayList<>();
        List<Integer> normToRawEnd = new ArrayList<>();
        int i = 0;
        while (i < cleaned.length()) {
            if (cleaned.charAt(i) == ' ') {
                appendMapped(out, normToCleaned, normToRawStart, normToRawEnd, ' ', i, base);
                i++;
                continue;
            }
            int j = i;
            while (j < cleaned.length() && cleaned.charAt(j) != ' ') {
                j++;
            }
            String token = cleaned.substring(i, j);
            String replacement = spelling.containsKey(token.toLowerCase()) ? spelling.get(token.toLowerCase()) : token.toLowerCase();
            for (int k = 0; k < replacement.length(); k++) {
                int cleanedIndex = i + Math.min(k, token.length() - 1);
                appendMapped(out, normToCleaned, normToRawStart, normToRawEnd, replacement.charAt(k), cleanedIndex, base);
            }
            i = j;
        }
        return new SpellingResult(out.toString(), toIntArray(normToCleaned), toIntArray(normToRawStart), toIntArray(normToRawEnd));
    }

    private static void appendMapped(StringBuilder out, List<Integer> normToCleaned, List<Integer> normToRawStart,
                                     List<Integer> normToRawEnd, char ch, int cleanedIndex, Result base) {
        out.append(ch);
        normToCleaned.add(cleanedIndex);
        normToRawStart.add(cleanedIndex < base.cleanedToRawStart.length ? base.cleanedToRawStart[cleanedIndex] : cleanedIndex);
        normToRawEnd.add(cleanedIndex < base.cleanedToRawEnd.length ? base.cleanedToRawEnd[cleanedIndex] : cleanedIndex + 1);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
