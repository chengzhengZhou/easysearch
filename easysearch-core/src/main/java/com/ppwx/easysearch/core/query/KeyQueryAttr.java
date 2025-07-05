package com.ppwx.easysearch.core.query;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className KeyQueryAttr
 * @description 查询关键字
 * @date 2024/10/14 19:21
 **/
public class KeyQueryAttr extends QueryAttr<String> {

    private String key;

    public KeyQueryAttr(String key) {
        this.key = key;
    }

    @Override
    public String getValue() {
        return key;
    }

}
