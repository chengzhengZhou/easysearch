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

package com.ppwx.easysearch.core.data.model;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataModelComposite;
import com.ppwx.easysearch.core.data.ListDataSet;
import com.ppwx.easysearch.core.data.element.StringColumn;
import com.ppwx.easysearch.core.pipeline.DataPipeline;
import com.ppwx.easysearch.core.pipeline.DataPipelineBuilder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * DataModelComposite测试类
 *
 * @since 1.0.0
 */
public class DataModelCompositeTest {

    private DataModel dataModel;

    @Before
    public void init() {
        DataModel dataModel1 = new MysqlDataModel(null);
        Attribute<String> name = dataModel1.attr(AttributeKey.valueOf("name"));
        name.set("sophiszhou");

        DataModel dataModel2 = new AbstractElasticSearchDataModel(null) {

            @Override
            protected SearchHits doQuery() throws IOException {
                return null;
            }
        };
        Attribute<String> name2 = dataModel2.attr(AttributeKey.valueOf("name"));
        name2.set("xuqin");
        Attribute<Integer> age = dataModel2.attr(AttributeKey.valueOf("age"));
        age.set(1);

        DataModel model = new DataModelComposite(Lists.newArrayList(dataModel1, dataModel2));
        this.dataModel = model;
    }

    @Test
    public void testAttrWorks() {
        boolean bool = dataModel.hasAttr(AttributeKey.valueOf("name"));
        Assert.assertTrue(bool);
        Attribute<Object> attr = dataModel.attr(AttributeKey.valueOf("name"));
        System.out.println(attr.get());
        attr = dataModel.attr(AttributeKey.valueOf("age"));
        System.out.println(attr.get());
        attr = dataModel.attr(AttributeKey.valueOf("name"));
        attr.set("xuqin");
        attr = dataModel.attr(AttributeKey.valueOf("name"));
        System.out.println(attr.get());

        attr = dataModel.attr(AttributeKey.valueOf("gender"));
        attr.set("女");
        attr = dataModel.attr(AttributeKey.valueOf("gender"));
        System.out.println(attr.get());
    }

    @Test
    public void testGetWorks() {
        MysqlDataModel model1 = new MysqlDataModel(null);
        Map<String, Column> map = new HashMap<>();
        map.put("id", new StringColumn("1"));
        map.put("name", new StringColumn("sophiszhou"));
        List<Map<String, Column>> data = Lists.newArrayList();
        data.add(map);
        data.add(map);
        model1.setDataSet(new ListDataSet(Lists.newArrayList("id", "name"), data));

        MysqlDataModel model2 = new MysqlDataModel(null);
        map = new HashMap<>();
        map.put("id", new StringColumn("2"));
        map.put("name", new StringColumn("xudashu"));
        data = Lists.newArrayList();
        data.add(map);
        data.add(map);
        model2.setDataSet(new ListDataSet(Lists.newArrayList("id", "name"), data));

        DataModelComposite modelComposite = new DataModelComposite(Lists.newArrayList(model1, model2));
        Map<String, Column> row = modelComposite.getDataset().row(0);
        System.out.println(row);
        row = modelComposite.getDataset().row(1);
        System.out.println(row);
        row = modelComposite.getDataset().row(3);
        System.out.println(row);
    }

    @Test
    public void testAddHandler() {
        DataPipelineBuilder builder = new DataPipelineBuilder();
        builder.dataModel(new LocalDataModel())
                .addHandler("userExposureFilteringHandler")
                .addHandler("premiumLimitationHandler")
                .addHandler("classIdInterleaveHandler")
                .addHandler("dataTruncateHandler")
        ;

        DataPipeline pipeline = builder.build();
        pipeline.fireDataComplete();
    }
}