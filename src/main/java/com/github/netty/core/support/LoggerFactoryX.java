package com.github.netty.core.support;

/**
 * Created by acer01 on 2018/8/25/025.
 */
public class LoggerFactoryX {

    public static LoggerX getLogger(Class clazz){
        return new LoggerX(org.slf4j.LoggerFactory.getLogger(clazz));
    }

}
