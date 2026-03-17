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

package com.ppwx.easysearch.core.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * SearchSession默认实现
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/29 12:01
 * @since 1.0.0
 */
public class DefaultSearchSession implements SearchSession, Serializable {
    /**
     * session id
     */
    protected String id;
    /**
     * session create time
     */
    private Long createTime;
    /**
     * last access time
     */
    private Long lastAccessTime;
    /**
     * inactive time
     */
    private Integer maxInactiveInterval;
    /**
     * key value map
     */
    private Map<String, Object> attributes = new HashMap<>();

    public DefaultSearchSession() {
    }

    public DefaultSearchSession(String id, Long createTime, Long lastAccessTime, Integer maxInactiveInterval) {
        this.id = id;
        this.createTime = createTime;
        this.lastAccessTime = lastAccessTime;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public long getCreationTime() {
        return this.createTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public Object getValue(String name) {
        return attributes.get(name);
    }

    @Override
    public String[] getValueNames() {
        Set<String> keySet = attributes.keySet();
        String[] arr = new String[keySet.size()];
        int i = 0;
        for (String key : keySet) {
            arr[i] = key;
            ++i;
        }
        return arr;
    }

    @Override
    public void putValue(String name, Object value) {
        this.attributes.put(name, value);
    }

    @Override
    public void removeValue(String name) {
        this.attributes.remove(name);
    }

    @Override
    public void invalidate() {
    }

    @Override
    public String toString() {
        return "DefaultSearchSession{" +
                "id='" + id + '\'' +
                ", createTime=" + createTime +
                ", lastAccessTime=" + lastAccessTime +
                ", maxInactiveInterval=" + maxInactiveInterval +
                ", attributes=" + attributes +
                '}';
    }
}