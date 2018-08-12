package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.Recyclable;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *
 * @author acer01
 *  2018/8/1/001
 */
public class HttpServletObject implements Recyclable{

    private static final AbstractRecycler<HttpServletObject> RECYCLER = new AbstractRecycler<HttpServletObject>() {
        @Override
        protected HttpServletObject newInstance() {
            return new HttpServletObject();
        }
    };

    private ServletHttpServletRequest httpServletRequest;
    private ServletHttpServletResponse httpServletResponse;
    private ChannelHandlerContext channelHandlerContext;
    private ServletContext servletContext;

    private HttpServletObject() {
    }

    public static HttpServletObject newInstance(ServletContext servletContext, ChannelHandlerContext context, FullHttpRequest fullHttpRequest) {
        HttpServletObject instance = RECYCLER.get();

        instance.servletContext = servletContext;
        instance.channelHandlerContext = context;
        instance.httpServletRequest = newHttpServletRequest(instance,fullHttpRequest);
        instance.httpServletResponse = newHttpServletResponse(instance);
        return instance;
    }

    /**
     * 创建新的servlet请求对象
     * @param httpServletObject
     * @param fullHttpRequest 完整的netty请求 (请求体 + 请求信息)
     * @return servlet 请求对象
     */
    private static ServletHttpServletRequest newHttpServletRequest(HttpServletObject httpServletObject, FullHttpRequest fullHttpRequest){
        NettyHttpRequest nettyRequest = NettyHttpRequest.newInstance(fullHttpRequest);
        ServletHttpServletRequest servletRequest = ServletHttpServletRequest.newInstance(httpServletObject,nettyRequest);
        return servletRequest;
    }

    /**
     * 创建新的servlet响应对象
     * @return servlet响应对象
     */
    private static ServletHttpServletResponse newHttpServletResponse(HttpServletObject httpServletObject){
        ServletHttpServletResponse servletResponse = ServletHttpServletResponse.newInstance(httpServletObject);
        return servletResponse;
    }

    public ServletHttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public ServletHttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public InetSocketAddress getServerSocketAddress(){
        return servletContext.getServerSocketAddress();
    }

    public InetSocketAddress getLocalAddress(){
        SocketAddress socketAddress = channelHandlerContext.channel().localAddress();
        if(socketAddress == null){
            return null;
        }
        if(socketAddress instanceof InetSocketAddress){
            return (InetSocketAddress) socketAddress;
        }
        return null;
    }

    public InetSocketAddress getRemoteAddress(){
        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        if(socketAddress == null){
            return null;
        }
        if(socketAddress instanceof InetSocketAddress){
            return (InetSocketAddress) socketAddress;
        }
        return null;
    }

    @Override
    public void recycle() {
        httpServletResponse.recycle();
        httpServletRequest.recycle();

        httpServletResponse = null;
        httpServletRequest = null;
        channelHandlerContext = null;
        servletContext = null;

        RECYCLER.recycle(this);
    }

}
