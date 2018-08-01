package com.github.netty.springboot;

import com.github.netty.core.adapter.AbstractChannelHandler;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import com.github.netty.servlet.support.HttpServletObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletDispatcherHandler extends AbstractChannelHandler<HttpServletObject> {

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, HttpServletObject httpServletObject) throws Exception {
        ServletHttpServletRequest httpServletRequest = httpServletObject.getHttpServletRequest();
        ServletHttpServletResponse httpServletResponse = httpServletObject.getHttpServletResponse();

        try {
            ServletRequestDispatcher dispatcher = httpServletRequest.getServletContext().getRequestDispatcher(httpServletRequest.getRequestURI());
            if (dispatcher == null) {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            dispatcher.dispatch(httpServletRequest, httpServletResponse, DispatcherType.REQUEST);
        }finally {
            httpServletObject.getHttpServletRequest().getInputStream().close();
            httpServletObject.getHttpServletResponse().getOutputStream().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.channel().close();
        }
    }


}
