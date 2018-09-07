package com.github.netty.rpc;

import java.lang.reflect.Method;

/**
 * 该接口可以重写rpc接口的方法
 *
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
