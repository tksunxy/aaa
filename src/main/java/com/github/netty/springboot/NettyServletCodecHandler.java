package com.github.netty.springboot;

import com.github.netty.core.adapter.NettyHttpRequest;
import com.github.netty.core.adapter.AbstractChannelHandler;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletInputStream;
import com.github.netty.util.HttpHeaderUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

/**
 * channel激活时， 开启一个新的输入流
 * 有信息/请求进入时，封装请求和响应对象，执行读操作
 * channel恢复时，关闭输入流，等待下一次连接到来
 * @author 84215
 */
public class NettyServletCodecHandler extends AbstractChannelHandler<HttpObject> {

    private ServletContext servletContext;
    // FIXME this feels wonky, need a better approach
    private ServletInputStream inputStream;

    public NettyServletCodecHandler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        inputStream = new ServletInputStream(ctx.channel());
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        //EmptyLastHttpContent, DefaultLastHttpContent
        if (msg instanceof HttpContent) {
            inputStream.addContent((HttpContent) msg);
        }

        if (msg instanceof HttpRequest) {
            NettyHttpRequest request = new NettyHttpRequest((HttpRequest) msg);

            if (HttpHeaderUtil.is100ContinueExpected(request)) { //请求头包含Expect: 100-continue
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE), ctx.voidPromise());
            }

            ServletHttpServletRequest servletRequest = newServletHttpServletRequest(request);

            ctx.fireChannelRead(servletRequest);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        inputStream.close();
    }

    private ServletHttpServletRequest newServletHttpServletRequest(NettyHttpRequest request){
        ServletHttpServletRequest servletRequest = new ServletHttpServletRequest(inputStream, servletContext, request);
//        ProxyUtil.setEnableProxy(true);
//        ServletHttpServletRequest servletRequest = ProxyUtil.newProxyByCglib(
//                ServletHttpServletRequest.class,
//                new Class[]{ServletInputStream.class,ServletContext.class,HttpRequest.class},
//                new Object[]{inputStream,servletContext,request});
//        ProxyUtil.setEnableProxy(false);
        return servletRequest;
    }

}