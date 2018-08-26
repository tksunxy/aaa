package com.github.netty.core;

import com.github.netty.core.support.ThreadFactoryX;
import com.github.netty.core.util.NamespaceUtil;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author 84215
 */
public class NioEventLoopBossGroup extends NioEventLoopGroup {

    private final String name;

    public NioEventLoopBossGroup() {
        this(0);
    }

    public NioEventLoopBossGroup(int nEventLoops) {
        this("",nEventLoops);
    }

    public NioEventLoopBossGroup(String preName,int nEventLoops) {
        super(nEventLoops,new ThreadFactoryX(preName, NioEventLoopBossGroup.class));
        this.name = NamespaceUtil.newIdName(preName,getClass());
    }

    @Override
    public String toString() {
        return name;
    }


}
