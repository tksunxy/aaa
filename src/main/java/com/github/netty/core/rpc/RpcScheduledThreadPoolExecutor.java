package com.github.netty.core.rpc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 84215
 */
public class RpcScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public RpcScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, new RpcThreadFactory(), new RpcRejectedExecutionHandler());
    }

    private static class RpcThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        RpcThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "Rpc-" + POOL_NUMBER.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(group, r,namePrefix + threadNumber.getAndIncrement(),0);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
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
