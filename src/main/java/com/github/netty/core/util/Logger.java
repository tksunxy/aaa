package com.github.netty.core.util;

/**
 * Created by acer01 on 2018/8/25/025.
 */
public class Logger {

    private static boolean isDisableLog = false;
    static {
//        isDisableLog = "1".equals(System.getProperty("log.disable"));
    }
    
    private org.slf4j.Logger logger;

    public Logger(org.slf4j.Logger logger) {
        if(isDisableLog){
            return;
        }
        this.logger = logger;
    }
    
    public void debug(String var1){
        if(isDisableLog){
            return;
        }
        logger.debug(var1);
    }
    
    public void debug(String var1,Throwable throwable){
        if(isDisableLog){
            return;
        }
        logger.debug(var1,throwable);
    }
    
    public void info(String var1){
        if(isDisableLog){
            return;
        }
        logger.info(var1);
    }

    public void error(String var1){
        if(isDisableLog){
            return;
        }
        logger.error(var1);
    }
    
    public void error(String var1,Throwable throwable){
        if(isDisableLog){
            return;
        }
        logger.error(var1,throwable);
    }
    
    public void warn(String var1){
        if(isDisableLog){
            return;
        }
        logger.warn(var1);
    }
    
}
