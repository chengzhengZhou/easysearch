/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: AbstractIncrementalModel
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/7/29 19:59
 * Description: 增量更新model
 */
package com.ppwx.easysearch.cf.model;

/**
 *
 * 增量更新model
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/07/29 19:59
 * @since 1.0.0
 */
public abstract class AbstractIncrementalModel implements Model {

    /**
     * 增量训练
     *
     * @param
     * @return void
     */
    public abstract void incrementalTrain();

    /**
     * 增量同步模型数据
     *
     * @param
     * @return void
     */
    public abstract void sync();

}