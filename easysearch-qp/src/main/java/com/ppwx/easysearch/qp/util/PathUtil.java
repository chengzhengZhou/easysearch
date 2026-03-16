package com.ppwx.easysearch.qp.util;

import cn.hutool.core.io.FileUtil;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className PathUtil
 * @description 文件路径加载工具
 * @date 2024/11/4 12:48
 **/
public final class PathUtil {

    private PathUtil() {

    }

    public static URL getUrl(String resourceLocation) {
        Assert.notNull(resourceLocation, "Resource location must not be null");
        if (resourceLocation.startsWith("classpath:")) {
            String path = resourceLocation.substring("classpath:".length());
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            return cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path);
        } else {
            try {
                return new URL(resourceLocation);
            } catch (MalformedURLException var6) {
                try {
                    return (FileUtil.file(resourceLocation)).toURI().toURL();
                } catch (MalformedURLException var5) {
                    return null;
                }
            }
        }
    }

}
