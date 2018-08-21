package com.github.netty.core.rpc;

import java.lang.reflect.Method;

/**
 * @author 84215
 */
@FunctionalInterface
public interface RpcOverrideMethod {

    /**
     * 执行方法
     * @param proxy
     * @param method
     * @param args
     * @return
     */
    Object invoke(Object proxy, Method method, Object[] args);

}
