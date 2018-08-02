package com.github.netty.springboot;

import com.github.netty.core.adapter.AbstractChannelHandler;
import com.github.netty.core.adapter.NettyHttpRequest;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletInputStream;
import com.github.netty.servlet.support.HttpServletObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.Objects;

/**
 * servlet解码器
 * @author 84215
 */
@ChannelHandler.Sharable
public class NettyServletCodecHandler extends AbstractChannelHandler<FullHttpRequest> {

    private ServletContext servletContext;

    public NettyServletCodecHandler(ServletContext servletContext) {
        this.servletContext = Objects.requireNonNull(servletContext);
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        //EmptyLastHttpContent, DefaultLastHttpContent

        ServletHttpServletRequest servletRequest = newServletHttpServletRequest(fullHttpRequest,ctx.channel());
        ServletHttpServletResponse servletResponse = newServletHttpServletResponse(ctx,servletRequest);
        HttpServletObject httpServletObject = new HttpServletObject(servletRequest,servletResponse);

        ctx.fireChannelRead(httpServletObject);
    }



    /**
     * 创建新的servlet请求对象
     * @param fullHttpRequest 完整的netty请求 (请求体 + 请求信息)
     * @param channel 连接
     * @return servlet请求对象
     */
    private ServletHttpServletRequest newServletHttpServletRequest(FullHttpRequest fullHttpRequest,Channel channel){
        ServletInputStream servletInputStream = new ServletInputStream(fullHttpRequest);
        NettyHttpRequest nettyRequest = new NettyHttpRequest(fullHttpRequest,channel);

        ServletHttpServletRequest servletRequest = new ServletHttpServletRequest(servletInputStream, servletContext, nettyRequest);
        return servletRequest;
    }

    /**
     * 创建新的servlet响应对象
     * @param ctx 业务链上下文
     * @param servletRequest servlet请求对象
     * @return servlet响应对象
     */
    private ServletHttpServletResponse newServletHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest){
        ServletHttpServletResponse servletResponse = new ServletHttpServletResponse(ctx,servletRequest);
        return servletResponse;
    }

}