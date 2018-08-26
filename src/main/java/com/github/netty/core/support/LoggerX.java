package com.github.netty.core.support;

/**
 * Created by acer01 on 2018/8/25/025.
 */
public class LoggerX {

    private org.slf4j.Logger logger;

    public LoggerX(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public void debug(String var1){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.debug(var1);
    }
    
    public void debug(String var1,Throwable throwable){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.debug(var1,throwable);
    }
    
    public void info(String var1){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.info(var1);
    }

    public void error(String var1){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.error(var1);
    }
    
    public void error(String var1,Throwable throwable){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.error(var1,throwable);
    }
    
    public void warn(String var1){
        if(!Optimize.isEnableLog()){
            return;
        }
        logger.warn(var1);
    }
    
}
