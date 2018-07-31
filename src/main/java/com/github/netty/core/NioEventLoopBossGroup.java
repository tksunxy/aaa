package com.github.netty.core;

import com.github.netty.util.NamespaceUtil;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author 84215
 */
public class NioEventLoopBossGroup extends NioEventLoopGroup {

    public NioEventLoopBossGroup() {
        super();
    }

    public NioEventLoopBossGroup(int nEventLoops) {
        super(nEventLoops);
    }

//    @Override
//    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
//        EventLoop eventLoop = super.newChild(executor, args);
//        String newName = toString()+"-"+ NamespaceUtil.newIdName(this,"nioEventLoop");
//        return ProxyUtil.newProxyByJdk(eventLoop,newName,true);
//    }


    @Override
    public String toString() {
        return NamespaceUtil.getIdNameClass(this,"boos");
    }


}
