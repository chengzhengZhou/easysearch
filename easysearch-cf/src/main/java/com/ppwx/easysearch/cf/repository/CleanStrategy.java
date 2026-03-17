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

package com.ppwx.easysearch.cf.repository;

import java.util.Date;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className CleanStrategy
 * @description 评分矩阵清理策略
 * @date 2024/10/23 19:13
 **/
public abstract class CleanStrategy {

    // 根据时间清除一定数量数据
    public static class TimeStrategy extends CleanStrategy {

        private final int size;

        private final Date begin;

        private final Date end;

        public TimeStrategy(int size, Date begin, Date end) {
            this.size = size;
            this.begin = begin;
            this.end = end;
        }

        public Date getBegin() {
            return begin;
        }

        public Date getEnd() {
            return end;
        }

        public int getSize() {
            return size;
        }
    }

    // 指定标的物id
    public static class ItemsStrategy extends CleanStrategy {
        private final String[] items;

        public ItemsStrategy(String[] items) {
            this.items = items;
        }

        public String[] getItems() {
            return items;
        }
    }

    // 清除表
    public static class TruncateStrategy extends CleanStrategy {

    }

}
