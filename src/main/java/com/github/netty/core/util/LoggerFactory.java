package com.github.netty.core.util;

/**
 * Created by acer01 on 2018/8/25/025.
 */
public class LoggerFactory {

    public static Logger getLogger(Class clazz){
        return new Logger(org.slf4j.LoggerFactory.getLogger(clazz));
    }

}
