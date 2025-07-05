/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: SessionManagerTest
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/29 16:40
 * Description: SessionManager测试类
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