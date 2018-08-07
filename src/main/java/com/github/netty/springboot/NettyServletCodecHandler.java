package com.github.netty.springboot;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.util.ProxyUtil;
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
        super(false);
        this.servletContext = Objects.requireNonNull(servletContext);
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        //EmptyLastHttpContent, DefaultLastHttpContent
        ChannelHandlerContext ctxWrap = PartialPooledByteBufAllocator.forceDirectAllocator(ctx);

        ServletHttpServletRequest servletRequest = newServletHttpServletRequest(fullHttpRequest,ctxWrap.channel());
        ServletHttpServletResponse servletResponse = newServletHttpServletResponse(ctxWrap,servletRequest);
        servletRequest.setHttpServletResponse(servletResponse);

        HttpServletObject httpServletObject = new HttpServletObject(servletRequest,servletResponse);

        ctxWrap.fireChannelRead(httpServletObject);
//        ctxWrap.fireChannelRead(new HttpServletObject());
    }

    /**
     * 创建新的servlet请求对象
     * @param fullHttpRequest 完整的netty请求 (请求体 + 请求信息)
     * @param channel 连接
     * @return servlet请求对象
     */
    private ServletHttpServletRequest newServletHttpServletRequest(FullHttpRequest fullHttpRequest,Channel channel){
        NettyHttpRequest nettyRequest = new NettyHttpRequest(fullHttpRequest,channel);
        if(ProxyUtil.isEnableProxy()){
            return ProxyUtil.newProxyByCglib(
                    ServletHttpServletRequest.class,
                    new Class[]{ServletContext.class,NettyHttpRequest.class},
                    new Object[]{servletContext,nettyRequest}
            );
        }else {
            return new ServletHttpServletRequest(servletContext,nettyRequest);
        }
    }

    /**
     * 创建新的servlet响应对象
     * @param ctx 业务链上下文
     * @param servletRequest servlet请求对象
     * @return servlet响应对象
     */
    private ServletHttpServletResponse newServletHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest){
        if(ProxyUtil.isEnableProxy()){
            return ProxyUtil.newProxyByCglib(
                    ServletHttpServletResponse.class,
                    new Class[]{ChannelHandlerContext.class,ServletHttpServletRequest.class},
                    new Object[]{ctx,servletRequest});
        }else {
            return new ServletHttpServletResponse(ctx,servletRequest);
        }
    }

}