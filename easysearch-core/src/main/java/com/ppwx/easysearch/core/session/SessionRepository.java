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
