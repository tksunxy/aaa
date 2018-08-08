package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.support.Recyclable;
import com.github.netty.servlet.ServletAsyncContext;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.Recycler;

/**
 *
 * @author acer01
 *  2018/8/1/001
 */
public class HttpServletObject implements Recyclable{

    private static final Recycler<HttpServletObject> RECYCLER = new Recycler<HttpServletObject>() {
        @Override
        protected HttpServletObject newObject(Handle<HttpServletObject> handle) {
            return new HttpServletObject(handle);
        }
    };
    private final Recycler.Handle<HttpServletObject> handle;

    private ServletHttpServletRequest httpServletRequest;
    private ServletHttpServletResponse httpServletResponse;

    private HttpServletObject(Recycler.Handle<HttpServletObject> handle) {
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
    private static ServletHttpServletResponse newHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest){
        if(ProxyUtil.isEnableProxy()){
            return ProxyUtil.newProxyByCglib(
                    ServletHttpServletResponse.class,
                    new Class[]{ChannelHandlerContext.class,ServletHttpServletRequest.class},
                    new Object[]{ctx,servletRequest});
        }else {
            return new ServletHttpServletResponse(ctx,servletRequest);
        }
    }

    public ServletHttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public ServletHttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    public void recycle() {
        try {
            httpServletRequest.getInputStream().close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        try {
            /*
             * 每个响应对象是只有当在servlet的service方法的范围内或在filter的doFilter方法范围内是有效的，除非该
             * 组件关联的请求对象已经开启异步处理。如果相关的请求已经启动异步处理，那么直到AsyncContext的
             * complete 方法被调用，请求对象一直有效。为了避免响应对象创建的性能开销，容器通常回收响应对象。
             * 在相关的请求的startAsync 还没有调用时，开发人员必须意识到保持到响应对象引用，超出之上描述的范
             * 围可能导致不确定的行为
             */
            ServletAsyncContext asyncContext = httpServletRequest.getAsyncContext();
            if (asyncContext == null || !asyncContext.isStarted()) {
                httpServletResponse.getOutputStream().close();
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        NettyHttpRequest nettyRequest = httpServletRequest.getNettyRequest();
        if(nettyRequest != null){
            nettyRequest.recycle();
        }

        NettyHttpResponse nettyResponse = httpServletResponse.getNettyResponse();
        if(nettyResponse != null){
            nettyResponse.recycle();
        }

        httpServletRequest = null;
        httpServletResponse = null;
        this.handle.recycle(this);
    }

}
