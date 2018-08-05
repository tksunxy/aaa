package com.github.netty.util;

/**
 * Created by acer01 on 2018/7/15/015.
 * 用于标注没有敲完,却提交git的代码, 相当于todo
 */
public @interface TodoOptimize {

    String value() default "";

}
