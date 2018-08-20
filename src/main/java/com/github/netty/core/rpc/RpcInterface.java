package com.github.netty.core.rpc;

import java.lang.annotation.*;

/**
 * Created by acer01 on 2018/8/20/020.
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
    int timeout() default 5;
}
