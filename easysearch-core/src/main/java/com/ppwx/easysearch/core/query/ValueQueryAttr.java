package com.ppwx.easysearch.core.query;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className KeyQueryAttr
 * @description 拓展处理关键字
 * @date 2024/10/14 19:21
 **/
public class ValueQueryAttr extends QueryAttr<ValueQueryAttr.Entry> {

    private final Entry entry;

    public ValueQueryAttr(String key, String value) {
        this.entry = new Entry(key, value);
    }

    @Override
    public ValueQueryAttr.Entry getValue() {
        return entry;
    }

    public static class Entry {
        private final String key;
        private final String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

}
