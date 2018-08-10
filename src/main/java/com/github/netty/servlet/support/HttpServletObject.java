package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 * @author acer01
 *  2018/8/1/001
 */
public class HttpServletObject implements Recyclable{

    private static final AbstractRecycler<HttpServletObject> RECYCLER = new AbstractRecycler<HttpServletObject>() {
        @Override
        protected HttpServletObject newInstance(Handle<HttpServletObject> handle) {
            return new HttpServletObject(handle);
        }
    };
    private final AbstractRecycler.Handle<HttpServletObject> handle;

    private ServletHttpServletRequest httpServletRequest;
    private ServletHttpServletResponse httpServletResponse;

    private HttpServletObject(AbstractRecycler.Handle<HttpServletObject> handle) {
        this.handle = handle;
    }

    public static HttpServletObject newInstance(ServletContext servletContext,ChannelHandlerContext ctx,FullHttpRequest fullHttpRequest) {
        HttpServletObject instance = RECYCLER.get();

        instance.httpServletRequest = newHttpServletRequest(servletContext,fullHttpRequest,ctx.channel());
        instance.httpServletResponse = newHttpServletResponse(ctx,instance.httpServletRequest);
        instance.httpServletRequest.setHttpServletResponse(instance.httpServletResponse);
        return instance;
    }

    /**
     * 创建新的servlet请求对象
     * @param servletContext
     * @param fullHttpRequest 完整的netty请求 (请求体 + 请求信息)
     * @param channel 连接
     * @return servlet 请求对象
     */
    private static ServletHttpServletRequest newHttpServletRequest(ServletContext servletContext, FullHttpRequest fullHttpRequest, Channel channel){
        NettyHttpRequest nettyRequest = NettyHttpRequest.newInstance(fullHttpRequest,channel);
        ServletHttpServletRequest servletRequest = ServletHttpServletRequest.newInstance(servletContext,nettyRequest);
        return servletRequest;
    }

    /**
     * 创建新的servlet响应对象
     * @param ctx 业务链上下文
     * @param servletRequest servlet请求对象
     * @return servlet响应对象
     */
    private static ServletHttpServletResponse newHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest){
        ServletHttpServletResponse servletResponse = ServletHttpServletResponse.newInstance(ctx,servletRequest);
        return servletResponse;
    }

    public ServletHttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public ServletHttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    public void recycle() {
//        Object oo = AbstractRecycler.getRecyclerList();
//        Object o =AbstractRecycler.getInstanceList();

        httpServletResponse.recycle();
        httpServletRequest.recycle();

        httpServletResponse = null;
        httpServletRequest = null;

        this.handle.recycle(this);
    }

}
