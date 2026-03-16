package com.ppwx.easysearch.qp.intervention;

import com.ppwx.easysearch.qp.source.PathTextLineSource;
import com.ppwx.easysearch.qp.source.TextLineSource;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 干预词：基于整句的改写引擎。
 * <p>
 * 格式约定（制表符分隔）：
 * 源句 \t 目标句 \t 匹配类型 \t 优先级
 * - 源句：用来匹配的原始 query 或短语
 * - 目标句：命中后改写成的 query
 * - 匹配类型：EXACT | PREFIX | CONTAINS，默认 EXACT
 * - 优先级：可选，默认为 0；命中多个时选优先级最高的一条
 */
public class SentenceInterventionEngine {

    private volatile List<SentenceRule> rules = new ArrayList<>();

    /**
     * 从路径加载整句干预规则。支持 classpath 资源路径和文件系统路径。
     */
    public void load(String path) throws IOException {
        load(new PathTextLineSource(path));
    }

    /**
     * 从统一资源源加载整句干预规则（如文件、数据库等）。
     */
    public void load(TextLineSource source) throws IOException {
        try (InputStream is = source.openStream()) {
            load(is);
        }
    }

    /**
     * 从输入流加载。格式：源句 \t 目标句 \t 匹配类型 \t 优先级
     */
    public void load(InputStream inputStream) {
        List<SentenceRule> newRules = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                SentenceRule rule = parseRule(line);
                if (rule != null) {
                    newRules.add(rule);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sentence intervention rules", e);
        }
        // 按优先级从高到低排序，优先匹配优先级高的规则
        newRules.sort(Comparator.comparingInt(SentenceRule::getPriority).reversed());
        rules = newRules;
    }

    private static SentenceRule parseRule(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 2) {
            return null;
        }
        String source = parts[0].trim();
        String target = parts[1].trim();
        if (source.isEmpty() || target.isEmpty()) {
            return null;
        }

        MatchType matchType = MatchType.EXACT;
        if (parts.length >= 3 && StringUtils.isNotBlank(parts[2])) {
            String type = parts[2].trim().toUpperCase();
            if ("PREFIX".equals(type)) {
                matchType = MatchType.PREFIX;
            } else if ("CONTAINS".equals(type)) {
                matchType = MatchType.CONTAINS;
            }
        }

        int priority = 0;
        if (parts.length >= 4 && StringUtils.isNotBlank(parts[3])) {
            try {
                priority = Integer.parseInt(parts[3].trim());
            } catch (NumberFormatException ignore) {
                // 使用默认优先级 0
            }
        }

        return new SentenceRule(source, target, matchType, priority);
    }

    /**
     * 按规则对整句 query 进行改写；若无命中则返回原 query。
     */
    public String rewrite(String query) {
        if (query == null) {
            return null;
        }
        List<SentenceRule> snapshot = rules;
        if (snapshot.isEmpty()) {
            return query;
        }

        for (SentenceRule rule : snapshot) {
            if (rule.matches(query)) {
                return rule.getTarget();
            }
        }
        return query;
    }

    /**
     * 当前是否已加载规则。
     */
    public boolean isLoaded() {
        return !rules.isEmpty();
    }

    /**
     * 匹配类型：EXACT / PREFIX / CONTAINS。
     */
    enum MatchType {
        EXACT,
        PREFIX,
        CONTAINS
    }

    /**
     * 整句干预规则。
     */
    static class SentenceRule {
        private final String source;
        private final String target;
        private final MatchType matchType;
        private final int priority;

        SentenceRule(String source, String target, MatchType matchType, int priority) {
            this.source = source;
            this.target = target;
            this.matchType = matchType;
            this.priority = priority;
        }

        String getSource() {
            return source;
        }

        String getTarget() {
            return target;
        }

        MatchType getMatchType() {
            return matchType;
        }

        int getPriority() {
            return priority;
        }

        boolean matches(String query) {
            if (query == null) {
                return false;
            }
            switch (matchType) {
                case PREFIX:
                    return query.startsWith(source);
                case CONTAINS:
                    return query.contains(source);
                case EXACT:
                default:
                    return query.equals(source);
            }
        }
    }
}

