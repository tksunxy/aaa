package com.github.netty.core.support;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 84215
 */
public class ThreadPoolX extends ScheduledThreadPoolExecutor {

    private static ThreadPoolX DEFAULT_INSTANCE;

    public static ThreadPoolX getDefaultInstance(){
        if(DEFAULT_INSTANCE == null){
            synchronized (ThreadPoolX.class){
                if(DEFAULT_INSTANCE == null){
                    DEFAULT_INSTANCE = new ThreadPoolX("Default",3);
                }
            }
        }
        return DEFAULT_INSTANCE;
    }

    public ThreadPoolX(int corePoolSize) {
        this("",corePoolSize);
    }

    public ThreadPoolX(String preName, int corePoolSize) {
        this(preName,corePoolSize,Thread.MAX_PRIORITY);
    }

    public ThreadPoolX(String preName, int corePoolSize, int priority) {
        super(corePoolSize, new ThreadFactoryX(preName,ThreadPoolX.class,priority), new RpcRejectedExecutionHandler());
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        //
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        //
    }

    private static class RpcRejectedExecutionHandler implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        private RpcRejectedExecutionHandler() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

}
