package com.ppwx.easysearch.core.session;

/**
 * 提供用于区分用户或调用客户端信息的容器
 * 相较于HttpSession，该Session只服务于搜推场景，会添加更丰富的方法和灵巧的调用机制
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/29 11:42
 * @since 1.0.0
 */
public interface SearchSession {


    /**
     * session创建时间，单位毫秒
     *
     * @param
     * @return long
     */
    long getCreationTime();

    /**
     * 会话id，唯一识别
     *
     * @param
     * @return java.lang.String
     */
    String getId();
    
    /**
     * 客户端最后一次请求的时间，单位毫秒
     *
     * @param 
     * @return long
     */
    long getLastAccessedTime();

    /**
     * 会话存活时间
     *
     * @param interval
     * @return void
     */
    void setMaxInactiveInterval(int interval);

    /**
     * 获取会话存活时间，单位秒
     *
     * @param
     * @return int
     */
    int getMaxInactiveInterval();

    /**
     * 获取绑定该session的字段，没有返回<code>null</code>
     *
     * @param name
     * @return java.lang.Object
     */
    Object getValue(String name);

    /**
     * 获取所有绑定该session的字段
     *
     * @param
     * @return java.lang.String[]
     */
    String[] getValueNames();

    /**
     * 绑定字段值
     *
     * @param name
     * @param value
     * @return void
     */
    void putValue(String name, Object value);

    /**
     * 移除字段
     *
     * @param name
     * @return void
     */
    void removeValue(String name);

    /**
     * 主动时效会话
     *
     * @param
     * @return void
     */
    void invalidate();

}
