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

package com.ppwx.easysearch.qp.support;

/**
 * Stage 抽象基类，提供名称管理和执行计时的模板方法。
 * <p>
 * 子类只需实现 {@link #doProcess(QueryContext)}，计时和 trace 写入由基类自动完成。
 */
public abstract class AbstractStage implements Stage {

    private final String name;

    protected AbstractStage(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public final void process(QueryContext ctx) {
        long startNanos = System.nanoTime();
        try {
            doProcess(ctx);
        } finally {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            ctx.putTrace(name + ".costMs", costMs);
        }
    }

    /**
     * 执行具体的处理逻辑，由子类实现。
     *
     * @param ctx 查询上下文
     */
    protected abstract void doProcess(QueryContext ctx);
}
