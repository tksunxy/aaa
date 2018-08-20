package com.github.netty.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  用于兼容 netty4 与netty5
 * @author 84215
 */
public abstract class AbstractChannelHandler<I> extends SimpleChannelInboundHandler<I> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractChannelHandler() {
        super();
    }

    protected AbstractChannelHandler(boolean autoRelease) {
        super(autoRelease);
    }

    protected void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception {
        onMessageReceived(ctx,msg);
    }

    protected void messageReceived(ChannelHandlerContext ctx, I msg) throws Exception {
        onMessageReceived(ctx,msg);
    }

    protected abstract void onMessageReceived(ChannelHandlerContext ctx, I msg) throws Exception;
}
