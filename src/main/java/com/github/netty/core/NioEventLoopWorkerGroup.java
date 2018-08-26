package com.github.netty.core;

import com.github.netty.core.support.ThreadFactoryX;
import com.github.netty.core.util.NamespaceUtil;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author 84215
 */
public class NioEventLoopWorkerGroup extends NioEventLoopGroup {

    private final String name;

    public NioEventLoopWorkerGroup() {
        this(0);
    }

    public NioEventLoopWorkerGroup(int nEventLoops) {
        this("",nEventLoops);
    }

    public NioEventLoopWorkerGroup(String preName,int nEventLoops) {
        super(nEventLoops,new ThreadFactoryX(preName, NioEventLoopWorkerGroup.class));
        this.name = NamespaceUtil.newIdName(preName,getClass());
    }

    @Override
    public String toString() {
        return name;
    }

}
