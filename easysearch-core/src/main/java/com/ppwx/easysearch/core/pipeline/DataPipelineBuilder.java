/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: DataPipelineBuilder
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/11 14:35
 * Description:
 */
package com.ppwx.easysearch.core.pipeline;

import com.google.common.collect.Lists;
import com.ppwx.easysearch.core.conf.Configuration;
import com.ppwx.easysearch.core.data.DataModel;
import com.ppwx.easysearch.core.data.DataModelComposite;
import com.ppwx.easysearch.core.util.SpringBeanFactory;
import io.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * 数据处理管道建造者
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/11 14:35
 * @since 1.0.0
 */
public class DataPipelineBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DataPipelineBuilder.class);

    private Configuration config;

    private DataModel dataModel;

    private Consumer<DataPipeline> consumer;

    private List<String> handlerNames;

    private DataPipeline pipeline;

    private boolean strictMode = true;
    /**
     * 配置
     * TODO 后续支持配置化
     *
     * @param config
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder config(Configuration config) {
        ObjectUtil.checkNotNull(config, "config");
        if (this.config != null) {
            throw new IllegalStateException("config set already");
        }
        this.config = config;
        return self();
    }

    /**
     * 数据模型
     *
     * @param dataModel
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder dataModel(DataModel dataModel) {
        ObjectUtil.checkNotNull(dataModel, "dataModel");
        if (this.dataModel != null) {
            throw new IllegalStateException("dataModel set already");
        }
        this.dataModel = dataModel;
        return self();
    }

    /**
     * 多个数据模型
     *
     * @param models
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder dataModel(DataModel... models) {
        ObjectUtil.checkNotNull(models, "models");
        if (this.dataModel != null) {
            throw new IllegalStateException("models set already");
        }
        if (models.length == 1) {
            return dataModel(models[0]);
        } else {
            this.dataModel = new DataModelComposite(Lists.newArrayList(models));
        }
        return self();
    }

    /**
     * 从容器中获取Handler然后依次添加到Pipeline中
     * 若需要个性化添加可使用 handler(java.util.function.Consumer) 方法
     *
     * @param handlerName
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder addHandler(String handlerName) {
        ObjectUtil.checkNotNull(handlerName, "handlerName");
        if (this.handlerNames == null) {
            this.handlerNames = Lists.newArrayList();
        }
        this.handlerNames.add(handlerName);
        return self();
    }

    /**
     * add handler consumer function
     *
     * @param consumer
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder handler(Consumer<DataPipeline> consumer) {
        ObjectUtil.checkNotNull(consumer, "consumer");
        if (this.consumer != null) {
            throw new IllegalStateException("consumer set already");
        }
        this.consumer = consumer;
        return self();
    }

    /**
     * 严格模式下会对多线程进行校验
     *
     * @param strict
     * @return com.ppwx.easysearch.core.pipeline.DataPipelineBuilder
     */
    public DataPipelineBuilder mode(boolean strict) {
        this.strictMode = strict;
        return self();
    }

    /**
     * check and build
     *
     * @param
     * @return com.ppwx.easysearch.core.pipeline.DataPipeline
     */
    public DataPipeline build() {
        if (this.pipeline != null) {
            return this.pipeline;
        }
        ObjectUtil.checkNotNull(this.dataModel, "dataModel");
        DataPipeline dataPipeline = new DefaultDataPipeline(dataModel, strictMode);

        if (!CollectionUtils.isEmpty(this.handlerNames)) {
            for (String name : this.handlerNames) {
                DataHandler handler = SpringBeanFactory.getObject(name, DataHandler.class);
                dataPipeline.addLast(name, handler);
            }
        }

        if (this.consumer != null) {
            this.consumer.accept(dataPipeline);
        }
        this.pipeline = dataPipeline;
        return this.pipeline;
    }

    private DataPipelineBuilder self() {
        return this;
    }
}