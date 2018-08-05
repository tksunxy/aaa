package com.github.netty.core.support;

/**
 * 包装者
 * Created by acer01 on 2018/7/31/031.
 */
public interface Wrapper<T> {

    /**
     * 包装
     * @param source 源对象
     */
    void wrap(T source);

    /**
     * 获取源对象
     * @return 源对象
     */
    T unwrap();

}
