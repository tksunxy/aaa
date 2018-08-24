package com.github.netty.core.rpc;

import java.lang.annotation.*;

/**
 * rpc接口 注:(要使用rpc, 必须接口上有这个注解)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcInterface {

    /**
     * 接口地址
     * @return
     */
    String value() default "";

    /**
     * 超时时间 (毫秒)
     * @return
     */
    int timeout() default 20;
}
