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

package com.ppwx.easysearch.qp.tokenizer.product3c;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 二手 3C 分词的特征列构造器。
 *
 * 列序与 data/template/3c_cws.tpl 对齐：
 * 0 char, 1 char_type, 2 lex_hit, 3 param_hit。
 */
public class FeatureColumnBuilder
{
    public static final int NUM_COLUMNS = 4;
    public static final String SPACE_FEATURE = "\\s";

    private static final String OUTSIDE = "O";
    private static final String LEX_PREFIX = "LEX";
    private static final String PARAM_PREFIX = "PARAM";
    private static final Set<Character> PUNCT_WHITELIST = new HashSet<Character>();
    private static final char REPLACEMENT_CHAR = '�';

    private static final Pattern[] PARAM_PATTERNS = new Pattern[]{
        Pattern.compile("(?i)\\d{1,4}(?:g|gb|t|tb)(?![a-z0-9])"),
        Pattern.compile("(?i)\\d{1,3}(?:g|gb|t|tb)?\\+\\d{1,4}(?:g|gb|t|tb)?(?![a-z0-9])"),
        Pattern.compile("(?i)(?<![a-z0-9])20\\d{2}(?:年|款)?(?![a-z0-9])"),
        Pattern.compile("(?<![0-9])\\d{1,2}新"),
        Pattern.compile("(?i)(?<![a-z0-9])\\d+(?:\\.\\d+)?(?:英寸|寸|″)(?![a-z0-9])"),
        Pattern.compile("(?i)(?<![a-z0-9])\\d+w(?![a-z0-9])"),
        Pattern.compile("(?i)(?:rtx|gtx|rx)\\s?(?:1050|1060|1070|1080|1650|1660|2060|2070|2080|3050|3060|3070|3080|3090|4050|4060|4070|4080|4090|5050|5060|5070|5080|5090|5500|5600|5700|5800|5900|6600|6700|6800|6900|7600|7700|7800|7900|960|970|980)(?:\\s?(?:ti|s|super|xt))?"),
        Pattern.compile("(?i)(?<![a-z0-9])(?:i[3579]-?\\d{3,5}[a-z]*|r[3579]-?\\d{3,5}[a-z]*|m[1-9](?:\\s?(?:pro|max|ultra))?)(?![a-z0-9])"),
        Pattern.compile("(?i)(?<![a-z0-9])\\d+(?:\\.\\d+)?mm(?![a-z0-9])"),
        Pattern.compile("(?i)(?<![a-z0-9])\\d{1,4}(?:\\.\\d+)?-\\d{1,4}(?:\\.\\d+)?mm(?![a-z0-9])"),
        Pattern.compile("(?i)(?<![a-z0-9])f/?\\d+(?:\\.\\d+)?(?:-\\d+(?:\\.\\d+)?)?(?![a-z0-9])")
    };

    private final List<String> lexicon = new ArrayList<String>();
    private final Map<String, String> spellingNormalizeMap = new HashMap<String, String>();

    static
    {
        for (char c : "+-/(). ".toCharArray())
        {
            PUNCT_WHITELIST.add(c);
        }
    }

    public FeatureColumnBuilder(String dictPath, String spellingPath) throws IOException
    {
        loadLexicon(dictPath);
        loadSpellingNormalize(spellingPath);
    }

    public Result build(String raw)
    {
        return build(raw, true);
    }

    public Result build(String raw, boolean applySpelling)
    {
        NormalizedText normalizedText = normalize(raw, applySpelling);
        return new Result(normalizedText.raw, normalizedText.text, normalizedText.charTypes,
                          normalizedText.rawStart, normalizedText.rawEnd, buildColumns(normalizedText));
    }

    /**
     * 返回按行组织的特征表：table[i][col] = 第 i 个字符的第 col 列特征。
     */
    public String[][] extractFeatureTable(String raw)
    {
        return toFeatureTable(build(raw, true).columns);
    }

    /**
     * 将列式特征转换为行式特征，兼容旧的调试接口与单元测试。
     */
    public static String[][] toFeatureTable(String[][] columns)
    {
        if (columns == null || columns.length == 0)
        {
            return new String[0][0];
        }
        int n = columns[0].length;
        String[][] table = new String[n][columns.length];
        for (int i = 0; i < n; i++)
        {
            for (int col = 0; col < columns.length; col++)
            {
                table[i][col] = columns[col][i];
            }
        }
        return table;
    }

    public void writeWordTags(String word, BufferedWriter bw) throws IOException
    {
        Result result = build(word, false);
        String normalized = result.normalized;
        if (normalized.length() == 0)
        {
            return;
        }
        if (normalized.length() == 1)
        {
            writeTrainingRow(result.columns, 0, "S", bw);
            return;
        }
        for (int i = 0; i < normalized.length(); i++)
        {
            String tag;
            if (i == 0)
            {
                tag = "B";
            }
            else if (i == normalized.length() - 1)
            {
                tag = "E";
            }
            else
            {
                tag = "M";
            }
            writeTrainingRow(result.columns, i, tag, bw);
        }
    }

    private void writeTrainingRow(String[][] columns, int index, String label, BufferedWriter bw) throws IOException
    {
        for (int col = 0; col < NUM_COLUMNS; col++)
        {
            if (col > 0)
            {
                bw.write('\t');
            }
            bw.write(columns[col][index]);
        }
        bw.write('\t');
        bw.write(label);
        bw.write('\n');
    }

    private String[][] buildColumns(NormalizedText normalizedText)
    {
        int n = normalizedText.length();
        String[][] columns = new String[NUM_COLUMNS][n];
        String[] lexHit = buildLexHit(normalizedText.text);
        String[] paramHit = buildParamHit(normalizedText.text);
        for (int i = 0; i < n; i++)
        {
            char ch = normalizedText.text.charAt(i);
            columns[0][i] = ch == ' ' ? SPACE_FEATURE : String.valueOf(ch);
            columns[1][i] = String.valueOf(normalizedText.charTypes[i]);
            columns[2][i] = lexHit[i];
            columns[3][i] = paramHit[i];
        }
        return columns;
    }

    private String[] buildLexHit(String text)
    {
        String[] tags = initTags(text.length());
        if (text.length() == 0 || lexicon.isEmpty())
        {
            return tags;
        }
        List<Span> spans = new ArrayList<Span>();
        for (String word : lexicon)
        {
            int from = 0;
            while (from < text.length())
            {
                int index = text.indexOf(word, from);
                if (index < 0)
                {
                    break;
                }
                spans.add(new Span(index, index + word.length()));
                from = index + 1;
            }
        }
        markNonOverlapping(tags, spans, LEX_PREFIX);
        return tags;
    }

    private String[] buildParamHit(String text)
    {
        String[] tags = initTags(text.length());
        List<Span> spans = new ArrayList<Span>();
        for (Pattern pattern : PARAM_PATTERNS)
        {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find())
            {
                if (matcher.end() > matcher.start())
                {
                    spans.add(new Span(matcher.start(), matcher.end()));
                }
            }
        }
        markNonOverlapping(tags, spans, PARAM_PREFIX);
        return tags;
    }

    private String[] initTags(int length)
    {
        String[] tags = new String[length];
        for (int i = 0; i < length; i++)
        {
            tags[i] = OUTSIDE;
        }
        return tags;
    }

    private void markNonOverlapping(String[] tags, List<Span> spans, String prefix)
    {
        Collections.sort(spans, new Comparator<Span>()
        {
            @Override
            public int compare(Span a, Span b)
            {
                if (a.start != b.start)
                {
                    return a.start - b.start;
                }
                return (b.end - b.start) - (a.end - a.start);
            }
        });
        boolean[] occupied = new boolean[tags.length];
        for (Span span : spans)
        {
            if (span.start < 0 || span.end > tags.length || span.start >= span.end)
            {
                continue;
            }
            boolean overlap = false;
            for (int i = span.start; i < span.end; i++)
            {
                if (occupied[i])
                {
                    overlap = true;
                    break;
                }
            }
            if (overlap)
            {
                continue;
            }
            for (int i = span.start; i < span.end; i++)
            {
                occupied[i] = true;
            }
            markSpan(tags, span.start, span.end, prefix);
        }
    }

    private void markSpan(String[] tags, int start, int end, String prefix)
    {
        int len = end - start;
        if (len == 1)
        {
            tags[start] = prefix + "-S";
            return;
        }
        tags[start] = prefix + "-B";
        for (int i = start + 1; i < end - 1; i++)
        {
            tags[i] = prefix + "-M";
        }
        tags[end - 1] = prefix + "-E";
    }

    private NormalizedText normalize(String raw, boolean applySpelling)
    {
        NormalizedBuilder builder = new NormalizedBuilder(raw);
        for (int i = 0; i < raw.length(); i++)
        {
            char original = raw.charAt(i);
            if (original == REPLACEMENT_CHAR)
            {
                continue;
            }
            String normalized = Normalizer.normalize(String.valueOf(original), Normalizer.Form.NFKC);
            for (int j = 0; j < normalized.length(); j++)
            {
                char ch = normalized.charAt(j);
                if (isAllowed(ch))
                {
                    builder.append(ch, charType(ch), i, i + 1);
                }
                else
                {
                    builder.appendSpace(i, i + 1);
                }
            }
        }
        NormalizedText normalizedText = builder.build();
        if (applySpelling && !spellingNormalizeMap.isEmpty())
        {
            normalizedText = applySpellingNormalize(normalizedText);
        }
        return normalizedText;
    }

    private boolean isAllowed(char ch)
    {
        return PUNCT_WHITELIST.contains(ch) || isChinese(ch) || Character.isLetterOrDigit(ch);
    }

    private boolean isChinese(char ch)
    {
        return ch >= '一' && ch <= '鿿';
    }

    private char charType(char ch)
    {
        if (ch == ' ')
        {
            return 'S';
        }
        if (isChinese(ch))
        {
            return 'C';
        }
        if (ch >= 'a' && ch <= 'z')
        {
            return 'L';
        }
        if (ch >= 'A' && ch <= 'Z')
        {
            return 'U';
        }
        if (Character.isDigit(ch))
        {
            return 'D';
        }
        return 'P';
    }

    private NormalizedText applySpellingNormalize(NormalizedText input)
    {
        NormalizedBuilder builder = new NormalizedBuilder(input.raw);
        int i = 0;
        while (i < input.length())
        {
            if (input.text.charAt(i) == ' ')
            {
                builder.appendSpace(input.rawStart[i], input.rawEnd[i]);
                i++;
                continue;
            }
            int j = i + 1;
            while (j < input.length() && input.text.charAt(j) != ' ')
            {
                j++;
            }
            String token = input.text.substring(i, j);
            String replacement = spellingNormalizeMap.get(token);
            if (replacement == null || nonSpaceLength(replacement) != token.length())
            {
                for (int k = i; k < j; k++)
                {
                    builder.append(input.text.charAt(k), input.charTypes[k], input.rawStart[k], input.rawEnd[k]);
                }
            }
            else
            {
                int sourceIndex = i;
                for (int k = 0; k < replacement.length(); k++)
                {
                    char ch = replacement.charAt(k);
                    if (ch == ' ')
                    {
                        int rawOffset = sourceIndex > i ? input.rawEnd[sourceIndex - 1] : input.rawStart[i];
                        builder.appendSpace(rawOffset, rawOffset);
                    }
                    else
                    {
                        char type = sourceIndex < j ? input.charTypes[sourceIndex] : charType(ch);
                        int rawStart = sourceIndex < j ? input.rawStart[sourceIndex] : input.rawStart[j - 1];
                        int rawEnd = sourceIndex < j ? input.rawEnd[sourceIndex] : input.rawEnd[j - 1];
                        builder.append(ch, type, rawStart, rawEnd);
                        sourceIndex++;
                    }
                }
            }
            i = j;
        }
        return builder.build();
    }

    private int nonSpaceLength(String text)
    {
        int length = 0;
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) != ' ')
            {
                length++;
            }
        }
        return length;
    }

    private void loadLexicon(String path) throws IOException
    {
        Set<String> words = new HashSet<String>();
        File file = new File(path);
        if (file.isDirectory())
        {
            String[] names = new String[]{"lexicon.txt", "brand.txt", "category.txt", "accessory.txt", "series.txt", "version.txt", "region.txt"};
            for (String name : names)
            {
                loadWordFile(new File(file, name), words);
            }
        }
        else
        {
            loadWordFile(file, words);
        }
        lexicon.clear();
        lexicon.addAll(words);
        Collections.sort(lexicon, new Comparator<String>()
        {
            @Override
            public int compare(String a, String b)
            {
                return b.length() - a.length();
            }
        });
    }

    private void loadWordFile(File file, Set<String> words) throws IOException
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#"))
                {
                    continue;
                }
                int tab = line.indexOf('\t');
                String word = (tab >= 0 ? line.substring(0, tab) : line).toLowerCase(Locale.ROOT);
                if (word.length() > 0)
                {
                    words.add(word);
                }
            }
        }
        finally
        {
            reader.close();
        }
    }

    private void loadSpellingNormalize(String path) throws IOException
    {
        spellingNormalizeMap.clear();
        File file = new File(path);
        if (!file.exists() || !file.isFile())
        {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#"))
                {
                    continue;
                }
                String[] parts = line.split("\\t");
                if (parts.length >= 2)
                {
                    spellingNormalizeMap.put(parts[0].toLowerCase(Locale.ROOT), parts[1].toLowerCase(Locale.ROOT));
                }
            }
        }
        finally
        {
            reader.close();
        }
    }

    public static class Result
    {
        public final String raw;
        public final String normalized;
        public final char[] charTypes;
        public final int[] rawStart;
        public final int[] rawEnd;
        /** columns[col][i] = 第 i 个字符的第 col 列特征。 */
        public final String[][] columns;

        Result(String raw, String normalized, char[] charTypes, int[] rawStart, int[] rawEnd, String[][] columns)
        {
            this.raw = raw;
            this.normalized = normalized;
            this.charTypes = charTypes;
            this.rawStart = rawStart;
            this.rawEnd = rawEnd;
            this.columns = columns;
        }

        public int length()
        {
            return normalized.length();
        }
    }

    private static class Span
    {
        final int start;
        final int end;

        Span(int start, int end)
        {
            this.start = start;
            this.end = end;
        }
    }

    private static class NormalizedText
    {
        final String raw;
        final String text;
        final char[] charTypes;
        final int[] rawStart;
        final int[] rawEnd;

        NormalizedText(String raw, String text, char[] charTypes, int[] rawStart, int[] rawEnd)
        {
            this.raw = raw;
            this.text = text;
            this.charTypes = charTypes;
            this.rawStart = rawStart;
            this.rawEnd = rawEnd;
        }

        int length()
        {
            return text.length();
        }
    }

    private static class NormalizedBuilder
    {
        private final String raw;
        private final StringBuilder text = new StringBuilder();
        private final List<Character> charTypes = new ArrayList<Character>();
        private final List<Integer> rawStart = new ArrayList<Integer>();
        private final List<Integer> rawEnd = new ArrayList<Integer>();

        NormalizedBuilder(String raw)
        {
            this.raw = raw;
        }

        void append(char ch, char type, int start, int end)
        {
            if (ch == ' ')
            {
                appendSpace(start, end);
                return;
            }
            text.append(Character.toLowerCase(ch));
            charTypes.add(type);
            rawStart.add(start);
            rawEnd.add(end);
        }

        void appendSpace(int start, int end)
        {
            if (text.length() == 0)
            {
                return;
            }
            if (text.charAt(text.length() - 1) == ' ')
            {
                int last = rawEnd.size() - 1;
                rawEnd.set(last, Math.max(rawEnd.get(last), end));
                return;
            }
            text.append(' ');
            charTypes.add('S');
            rawStart.add(start);
            rawEnd.add(end);
        }

        NormalizedText build()
        {
            if (text.length() > 0 && text.charAt(text.length() - 1) == ' ')
            {
                text.setLength(text.length() - 1);
                charTypes.remove(charTypes.size() - 1);
                rawStart.remove(rawStart.size() - 1);
                rawEnd.remove(rawEnd.size() - 1);
            }
            char[] typeArray = new char[charTypes.size()];
            int[] startArray = new int[rawStart.size()];
            int[] endArray = new int[rawEnd.size()];
            for (int i = 0; i < typeArray.length; i++)
            {
                typeArray[i] = charTypes.get(i);
                startArray[i] = rawStart.get(i);
                endArray[i] = rawEnd.get(i);
            }
            return new NormalizedText(raw, text.toString(), typeArray, startArray, endArray);
        }
    }
}
