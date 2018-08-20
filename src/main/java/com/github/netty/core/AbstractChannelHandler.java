package com.github.netty.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
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

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if(evt instanceof IdleStateEvent){
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    onReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    onWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    onAllIdle(ctx);
                    break;
                default:
                    break;
            }
        }
    }

    protected void onAllIdle(ChannelHandlerContext ctx){

    }

    protected void onWriterIdle(ChannelHandlerContext ctx){

    }

    protected void onReaderIdle(ChannelHandlerContext ctx){

    }

}
