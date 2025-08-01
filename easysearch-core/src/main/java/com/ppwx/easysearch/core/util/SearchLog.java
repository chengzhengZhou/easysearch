/**
 * Copyright (C), 2010-2024, 爱回收
 * FileName: SearchLog
 * Author:   Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * Date:     2024/2/27 10:08
 * Description: 统一日志
 */
package com.ppwx.easysearch.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 *
 * 统一日志
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2024/02/27 10:08
 * @since 1.0.0
 */
public final class SearchLog implements Logger {
    /**
     * logger
     */
    private final Logger logger;
    /**
     * singleton instance
     */
    private static final SearchLog instance;

    /**
     * create instance
     */
    static {
        instance = new SearchLog(LoggerFactory.getLogger("SearchLog"));
    }

    /**
     * private constructor
     *
     * @param logger
     * @return
     */
    private SearchLog(Logger logger) {
        this.logger = logger;
    }

    /**
     * return inner instance
     *
     * @param
     * @return com.ppwx.easysearch.core.util.SearchLog
     */
    public static SearchLog getLogger() {
        return instance;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(s);
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        logger.trace(marker, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(marker, s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(marker, s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(marker, s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        if (logger.isDebugEnabled()) {
            logger.debug(s);
        }
    }

    @Override
    public void debug(String s, Object o) {
        if (logger.isDebugEnabled()) {
            logger.debug(s, o);
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        if (logger.isDebugEnabled()) {
            logger.debug(s, o, o1);
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        if (logger.isDebugEnabled()) {
            logger.debug(s, objects);
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.debug(s, throwable);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, s);
        }
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, s, o);
        }
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, s, o, o1);
        }
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, s, objects);
        }
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, s, throwable);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object o) {
        logger.info(s, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(s, o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(marker, s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(marker, s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker, s, o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker, s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(s, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(marker, s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(marker, s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(marker, s, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(marker, s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(marker, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Object o) {
        logger.error(s, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(s, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(marker, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(marker, s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(marker, s, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(marker, s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(marker, s, throwable);
    }
}