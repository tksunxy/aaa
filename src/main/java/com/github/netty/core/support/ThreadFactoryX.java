package com.github.netty.core.support;

import com.github.netty.core.util.NamespaceUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Created by acer01 on 2018/8/25/025.
 */
public class ThreadFactoryX extends DefaultThreadFactory implements java.util.concurrent.ThreadFactory {

    private final String preName;

    public ThreadFactoryX(String preName, Class<?> poolType) {
        this(preName,poolType, Thread.MAX_PRIORITY);
    }

    public ThreadFactoryX(String preName, Class<?> poolType, int priority) {
        super(NamespaceUtil.newIdName(poolType), priority);
        this.preName = preName;
    }

    public ThreadFactoryX(String poolName, String preName) {
        super(poolName);
        this.preName = preName;
    }

    @Override
    protected Thread newThread(Runnable r, String name) {
        Thread thread = super.newThread(r, name);
        if(preName != null && preName.length() > 0) {
            thread.setName(preName + "-" + thread.getName());
        }
        return thread;
    }
}
