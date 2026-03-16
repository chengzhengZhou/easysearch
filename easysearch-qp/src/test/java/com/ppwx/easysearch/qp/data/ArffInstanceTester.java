package com.ppwx.easysearch.qp.data;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ArffInstanceTester
 * @description todo
 * @date 2025/1/22 14:25
 **/
public class ArffInstanceTester {

    @Test
    public void testArffWorks() {
        ArrayList<ArffAttribute > attrs = Lists.newArrayListWithCapacity(4);
        ArrayList<String> instanceData = Lists.newArrayListWithCapacity(4);
        attrs.add(new ArffAttribute("id", "INTEGER", 0));
        instanceData.add(0, "1");
        attrs.add(new ArffAttribute("name", "STRING", 1));
        instanceData.add(1, "next.getName()");
        attrs.add(new ArffAttribute("img", "STRING", 2));
        instanceData.add(2, "next.getPicUrl()");
        attrs.add(new ArffAttribute("minPrice", "LONG", 3));
        instanceData.add(3, null);
        ArffInstance instance = new ArffInstance(instanceData, attrs);
        Assert.assertNull(instance.getValueByAttrName("minPrice"));
        System.out.println(instance.getValueByAttrName("img"));
    }

}
