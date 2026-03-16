package com.ppwx.easysearch.qp.synonym;

import com.ppwx.easysearch.qp.source.TextLineSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 同义词门面：封装 SynonymEngine + 默认改写/拓展策略，提供 match、rewrite、expand、reload。
 */
public class SynonymService {

    private final SynonymEngine engine;
    private volatile RewriteStrategy defaultRewriteStrategy;
    private volatile ExpandStrategy defaultExpandStrategy;

    public SynonymService(SynonymEngine engine) {
        this.engine = engine;
        this.defaultRewriteStrategy = new ReplaceFirstRewriteStrategy();
        this.defaultExpandStrategy = new ExpandOrStrategy();
    }

    /**
     * 创建并加载同义词表的服务（从路径）。
     */
    public static SynonymService create(String synonymPath) throws IOException {
        SynonymEngine engine = new SynonymEngine();
        engine.load(synonymPath);
        return new SynonymService(engine);
    }

    /**
     * 从统一资源源创建并加载同义词表（如文件、数据库等）。
     */
    public static SynonymService create(TextLineSource source) throws IOException {
        SynonymEngine engine = new SynonymEngine();
        engine.load(source);
        return new SynonymService(engine);
    }

    public void setDefaultRewriteStrategy(RewriteStrategy strategy) {
        this.defaultRewriteStrategy = strategy != null ? strategy : new ReplaceFirstRewriteStrategy();
    }

    public void setDefaultExpandStrategy(ExpandStrategy strategy) {
        this.defaultExpandStrategy = strategy != null ? strategy : new ExpandOrStrategy();
    }

    /**
     * 匹配同义词，返回不重叠的最长匹配列表。
     */
    public List<SynonymMatch> match(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }
        return engine.match(query);
    }

    /**
     * 使用默认改写策略改写 query。
     */
    public String rewrite(String query) {
        return rewrite(query, null);
    }

    /**
     * 改写 query；若 strategy 为 null 则使用默认策略。
     */
    public String rewrite(String query, RewriteStrategy strategy) {
        if (query == null) {
            return null;
        }
        List<SynonymMatch> matches = engine.match(query);
        if (matches.isEmpty()) {
            return query;
        }
        RewriteStrategy s = strategy != null ? strategy : defaultRewriteStrategy;
        return s.rewrite(query, matches);
    }

    /**
     * 使用默认拓展策略拓展 query。
     */
    public List<String> expand(String query) {
        return expand(query, null);
    }

    /**
     * 拓展 query；若 strategy 为 null 则使用默认策略。
     */
    public List<String> expand(String query, ExpandStrategy strategy) {
        if (query == null) {
            return Collections.singletonList(null);
        }
        List<SynonymMatch> matches = engine.match(query);
        ExpandStrategy s = strategy != null ? strategy : defaultExpandStrategy;
        return s.expand(query, matches);
    }

    /**
     * 重新加载同义词表（从路径）。
     */
    public void reload(String synonymPath) throws IOException {
        engine.load(synonymPath);
    }

    /**
     * 从统一资源源重新加载同义词表。
     */
    public void reload(TextLineSource source) throws IOException {
        engine.load(source);
    }

    public SynonymEngine getEngine() {
        return engine;
    }
}
