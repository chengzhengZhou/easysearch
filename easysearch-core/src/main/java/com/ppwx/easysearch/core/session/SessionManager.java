/**
 * Copyright (C), 2010-2023, 爱回收
 * FileName: SessionManager
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2023/12/29 12:03
 * Description: 会话管理器
 */
package com.ppwx.easysearch.core.session;

import com.ppwx.easysearch.core.util.CurrentTimeUtil;

import java.util.UUID;

/**
 *
 * 会话管理器
 * 提供会话的获取、失效、持久化等
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/29 12:03
 * @since 1.0.0
 */
public class SessionManager {

    /**
     * global session inactive time
     */
    private static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 30 * 60;
    /**
     * 持久化
     */
    private SessionRepository repository;
    /**
     * 持久化策略
     */
    private FlushMode flushMode = FlushMode.LAZY;
    /**
     * inactive time
     */
    private int maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    public void setRepository(SessionRepository repository) {
        this.repository = repository;
    }

    public FlushMode getFlushMode() {
        return flushMode;
    }

    public void setFlushMode(FlushMode flushMode) {
        this.flushMode = flushMode;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    /**
     * 获取session
     *
     * @param sessionId
     * @return com.ppwx.easysearch.core.session.SearchSession
     */
    public SearchSession getSession(String sessionId) {
        return getSession(sessionId, true);
    }

    /**
     * 获取session
     * 参数<code>created</code>可配置是否创建
     *
     * @param sessionId
     * @param created
     * @return com.ppwx.easysearch.core.session.SearchSession
     */
    public SearchSession getSession(String sessionId, boolean created) {
        DefaultSearchSession session = repository.getSession(sessionId);
        if (session != null) {
            session.setLastAccessTime(CurrentTimeUtil.currentTimeMillis());
            return new SessionDecorator(session, this.flushMode);
        }
        return created ? newSession() : null;
    }

    /**
     * 创建会话
     *
     * @param
     * @return com.ppwx.easysearch.core.session.SearchSession
     */
    public SearchSession newSession() {
        DefaultSearchSession session = new DefaultSearchSession();
        session.id = genUid();
        session.setCreateTime(CurrentTimeUtil.currentTimeMillis());
        session.setLastAccessTime(CurrentTimeUtil.currentTimeMillis());
        session.setMaxInactiveInterval(maxInactiveInterval);
        // pre save
        saveToRepositoryImmediately(session);

        return new SessionDecorator(session, flushMode);
    }

    /**
     * 主动提交保存
     *
     * @param session
     * @return void
     */
    public void commit(SearchSession session) {
        if (session == null) {
            return;
        }
        DefaultSearchSession defaultSearchSession;
        if (session instanceof SessionDecorator) {
            defaultSearchSession = ((SessionDecorator) session).targetSession;
        } else if (session instanceof DefaultSearchSession) {
            defaultSearchSession = (DefaultSearchSession) session;
        } else {
            // 暂不支持的自定义session，避免出现序列化等不支持
            return;
        }
        saveToRepositoryImmediately(defaultSearchSession);
    }

    void saveToRepositoryImmediately(DefaultSearchSession session) {
        repository.updateSession(session);
    }

    /**
     * 会话id生成
     *
     * @param
     * @return java.lang.String
     */
    private String genUid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private class SessionDecorator implements SearchSession {

        private DefaultSearchSession targetSession;

        private boolean invalidated = false;

        private FlushMode flushMode;

        public SessionDecorator(DefaultSearchSession targetSession, FlushMode flushMode) {
            this.targetSession = targetSession;
            this.flushMode = flushMode;
        }

        @Override
        public long getCreationTime() {
            checkState();
            return targetSession.getCreationTime();
        }

        @Override
        public String getId() {
            checkState();
            return targetSession.getId();
        }

        @Override
        public long getLastAccessedTime() {
            checkState();
            return targetSession.getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            checkState();
            targetSession.setMaxInactiveInterval(interval);
        }

        @Override
        public int getMaxInactiveInterval() {
            checkState();
            return targetSession.getMaxInactiveInterval();
        }

        @Override
        public Object getValue(String name) {
            checkState();
            return targetSession.getValue(name);
        }

        @Override
        public String[] getValueNames() {
            checkState();
            return targetSession.getValueNames();
        }

        @Override
        public void putValue(String name, Object value) {
            checkState();
            targetSession.putValue(name, value);
        }

        @Override
        public void removeValue(String name) {
            checkState();
            targetSession.removeValue(name);
        }

        @Override
        public void invalidate() {
            checkState();
            this.invalidated = true;
            repository.removeSession(targetSession.getId());
        }

        private void flushToRepository() {
            if (flushMode == FlushMode.LAZY) {
                return;
            }
            saveToRepositoryImmediately(targetSession);
        }

        private void checkState() {
            if (this.invalidated) {
                throw new IllegalStateException("Session is invalidated!");
            }
        }
    }
}