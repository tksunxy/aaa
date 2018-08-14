package com.github.netty.core;

import com.github.netty.core.util.NamespaceUtil;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author 84215
 */
public class NioEventLoopWorkerGroup extends NioEventLoopGroup {

    public NioEventLoopWorkerGroup() {
        super();
    }

    public NioEventLoopWorkerGroup(int nEventLoops) {
        super(nEventLoops);
    }

//    @Override
//    protected EventExecutor newChild(ThreadFactory threadFactory, Object... args) throws Exception {
//        return super.newChild(threadFactory, args);
//    }
//
//    @Override
//    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
//        EventLoop eventLoop = super.newChild(executor, args);
//        String newName = toString()+ "-" + NamespaceUtil.newIdName(this, "nioEventLoop");
//        return ProxyUtil.newProxyByJdk(eventLoop,newName ,true);
//    }


    @Override
    public String toString() {
        return NamespaceUtil.getIdNameClass(this,"worker");
    }

}
