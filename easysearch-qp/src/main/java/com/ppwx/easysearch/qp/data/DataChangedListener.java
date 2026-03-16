package com.ppwx.easysearch.qp.data;

public interface DataChangedListener<T> extends ResourceLoader {

    /**
     * 添加资源项
     *
     * @param termOpt 资源项
     */
    void addItem(T termOpt);

    /**
     * 更新资源项
     *
     * @param termOpt 更新项
     */
    void updateItem(T termOpt);

    /**
     * 删除资源项
     *
     * @param termOpt 删除项
     */
    void deleteItem(T termOpt);

}
