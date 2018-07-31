package com.github.netty.springboot;

import com.github.netty.core.adapter.ChannelHandlerVersionAdapter;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletDispatcherHandler extends ChannelHandlerVersionAdapter<ServletHttpServletRequest> {

    private ServletContext servletContext;

    public NettyServletDispatcherHandler(ServletContext servletContext) {
        super();
        this.servletContext = servletContext;
    }

    @Override
    protected void adaptMessageReceived(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest) throws Exception {
        HttpRequest nettyRequest = servletRequest.getNettyRequest();
        ServletHttpServletResponse servletResponse = newServletHttpServletResponse(ctx,servletRequest);

        try {
            ServletRequestDispatcher dispatcher = servletContext.getRequestDispatcher(servletRequest.getRequestURI());
            if (dispatcher == null) {
                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            dispatcher.dispatch(servletRequest, servletResponse, DispatcherType.REQUEST);
        } finally {
            if (!servletRequest.isAsyncStarted()) {
                servletResponse.getOutputStream().close();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.close();
        }
    }

    private ServletHttpServletResponse newServletHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest){
        ServletHttpServletResponse servletResponse = new ServletHttpServletResponse(ctx,servletRequest);
//        ServletHttpServletResponse servletResponse = ProxyUtil.newProxyByCglib(
//                ServletHttpServletResponse.class,
//                new Class[]{NettyFullHttpResponse.class,ServletHttpServletRequest.class},
//                new Object[]{nettyResponse,servletRequest});
        return servletResponse;
    }

}
