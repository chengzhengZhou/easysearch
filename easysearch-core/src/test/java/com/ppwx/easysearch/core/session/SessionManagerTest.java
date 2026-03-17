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

import com.ppwx.easysearch.core.TestLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * SessionManager测试类
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/29 16:40
 * @since 1.0.0
 */
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void init() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.setFlushMode(FlushMode.LAZY);
        sessionManager.setRepository(new SessionRepository() {
            @Override
            public DefaultSearchSession getSession(String sessionId) {
                TestLogger.info("getSession：{}", sessionId);
                return null;
            }

            @Override
            public void updateSession(DefaultSearchSession session) {
                TestLogger.info("updateSession：{}", session);
            }

            @Override
            public void removeSession(String sessionId) {
                TestLogger.info("removeSession：{}", sessionId);
            }
        });
        this.sessionManager = sessionManager;
    }

    @Test
    public void testSessionManagerWorks() {
        SearchSession session = sessionManager.newSession();
        session.putValue("name", "sophiszhou");
        sessionManager.commit(session);
        session.invalidate();
    }

    @Test
    public void testImmediateWorks() {
        sessionManager.setFlushMode(FlushMode.IMMEDIATE);
        SearchSession session = sessionManager.getSession("123");
        session.putValue("name", "sophiszhou");
        String id = session.getId();
        Assert.assertNotNull(id);
    }

}