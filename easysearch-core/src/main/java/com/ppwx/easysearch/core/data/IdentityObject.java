package com.ppwx.easysearch.core.data;

/**
 * 标识对象唯一
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/06 16:08
 * @since 1.0.0
 */
public interface IdentityObject<K extends Comparable> extends Comparable<IdentityObject> {

    K getId();

    default int compareTo(IdentityObject that) {
        int compare = this.getClass().getName().compareTo(that.getClass().getName());
        return compare == 0 ? this.getId().compareTo(that.getId()) : compare;
    }

}
