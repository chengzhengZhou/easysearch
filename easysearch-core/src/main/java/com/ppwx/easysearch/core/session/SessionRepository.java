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

/**
 * session 持久化
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/29 14:21
 * @since 1.0.0
 */
public interface SessionRepository {

    /**
     * 获取会话
     *
     * @param sessionId
     * @return com.ppwx.easysearch.core.session.DefaultSearchSession
     */
    DefaultSearchSession getSession(String sessionId);

    /**
     * 更新会话
     *
     * @param session
     * @return void
     */
    void updateSession(DefaultSearchSession session);

    /**
     * 删除
     *
     * @param sessionId
     * @return void
     */
    void removeSession(String sessionId);

}
