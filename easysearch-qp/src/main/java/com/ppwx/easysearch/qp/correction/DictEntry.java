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

package com.ppwx.easysearch.qp.correction;

/**
 * 纠错词典条目，包含词、拼音和词频。
 */
public class DictEntry {

    private final String word;
    private final String pinyin;
    private final long frequency;

    public DictEntry(String word, String pinyin, long frequency) {
        this.word = word;
        this.pinyin = pinyin;
        this.frequency = frequency;
    }

    public DictEntry(String word, String pinyin) {
        this(word, pinyin, 1L);
    }

    public String getWord() {
        return word;
    }

    public String getPinyin() {
        return pinyin;
    }

    public long getFrequency() {
        return frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DictEntry that = (DictEntry) o;
        return word.equals(that.word);
    }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    @Override
    public String toString() {
        return "DictEntry{word='" + word + "', pinyin='" + pinyin + "', freq=" + frequency + "}";
    }
}
