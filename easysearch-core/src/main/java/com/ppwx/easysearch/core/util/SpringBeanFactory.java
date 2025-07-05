package com.ppwx.easysearch.core.util;

import java.util.Map;

/**
 *
 * Get bean from spring ioc container.
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/06 15:38
 * @since 1.0.0
 */
public class SpringBeanFactory {

    @SuppressWarnings("unchecked")
    public static <T> T getObject(String name) {
        return (T) ContextHolder.getApplicationContext().getBean(name);
    }

    public static <T> T getObject(Class<? extends T> classz) {
        return (T) ContextHolder.getApplicationContext().getBean(classz);
    }

    public static <T> T getObject(String name, Class<? extends T> classz) {
        return (T) ContextHolder.getApplicationContext().getBean(name, classz);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return ContextHolder.getApplicationContext().getBeansOfType(clazz);
    }
}
