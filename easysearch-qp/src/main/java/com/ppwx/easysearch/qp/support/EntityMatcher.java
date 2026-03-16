package com.ppwx.easysearch.qp.support;

import com.ppwx.easysearch.qp.support.FoundWord;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface Matcher
 * @description 命名实体匹配识别
 * @date 2024/10/9 15:48
 **/
public interface EntityMatcher extends Ordered {

    /**
     * @description 匹配识别
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/9 15:57
     * @param val
     * @return List<FoundWord>
    */
    List<FoundWord> matchAll(String val);

    /**
     * @description 识别名称
     * @author ext.ahs.zhouchzh1@jd.com
     * @date 2024/10/9 17:39
     * @return String
    */
    String getName();
}
