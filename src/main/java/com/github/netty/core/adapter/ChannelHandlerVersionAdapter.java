package com.github.netty.core.adapter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author 84215
 */
public class ChannelHandlerVersionAdapter<I> extends SimpleChannelInboundHandler<I> {

    protected void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception {
        adaptMessageReceived(ctx,msg);
    }

    protected void messageReceived(ChannelHandlerContext ctx, I msg) throws Exception {
        adaptMessageReceived(ctx,msg);
    }

    protected void adaptMessageReceived(ChannelHandlerContext ctx, I msg) throws Exception {

    }
}
