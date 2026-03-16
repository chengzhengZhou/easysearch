package com.ppwx.easysearch.qp.ner;

import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @interface WordMerger
 * @description 合并词，分类词典词设置教细腻时会导致拆分过细，需要合并同类项
 * @date 2024/10/9 19:29
 **/
public interface WordMerger {

    List<FindEntity> merge(String val, List<FindEntity> origin);

}
