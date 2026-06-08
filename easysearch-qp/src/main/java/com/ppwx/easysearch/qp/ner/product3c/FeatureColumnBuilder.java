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

import java.io.IOException;
import java.util.Map;

/**
 * 构造 Product3C CRF 所需的 6 列字符级特征。
 * 列序：0 小写字符 / 1 字类 / 2 品牌命中 / 3 品类命中 / 4 参数模式 / 5 系列命中
 */
public class FeatureColumnBuilder {

    private final Map<String, String> spellingMap;
    private final GazetteerMatcher gazetteer;

    public FeatureColumnBuilder(String dictDir, String spellingPath) throws IOException {
        this.spellingMap = Product3CTextNormalizer.loadSpellingMap(spellingPath);
        this.gazetteer = new GazetteerMatcher(dictDir);
    }

    public BuildResult build(String rawQuery) {
        Product3CTextNormalizer.Result base = Product3CTextNormalizer.normalize(rawQuery);
        Product3CTextNormalizer.SpellingResult spell = Product3CTextNormalizer.applySpelling(base, spellingMap);
        String[][] columns = gazetteer.buildColumns(base, spell);
        return new BuildResult(base, spell, columns);
    }

    public GazetteerMatcher getGazetteer() {
        return gazetteer;
    }

    public static class BuildResult {
        private final Product3CTextNormalizer.Result base;
        private final Product3CTextNormalizer.SpellingResult spell;
        private final String[][] columns;

        BuildResult(Product3CTextNormalizer.Result base, Product3CTextNormalizer.SpellingResult spell, String[][] columns) {
            this.base = base;
            this.spell = spell;
            this.columns = columns;
        }

        public Product3CTextNormalizer.Result getBase() {
            return base;
        }

        public Product3CTextNormalizer.SpellingResult getSpell() {
            return spell;
        }

        public String[][] getColumns() {
            return columns;
        }
    }
}
