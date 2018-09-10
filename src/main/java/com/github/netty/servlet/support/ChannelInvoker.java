package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpCookie;
import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.HttpHeaderUtil;
import com.github.netty.core.util.RecyclableUtil;
import com.github.netty.core.util.StringUtil;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletHttpSession;
import com.github.netty.servlet.ServletSessionCookieConfig;
import com.github.netty.servlet.util.ServletUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;

import javax.servlet.http.Cookie;
import java.util.List;

/**
 *
 * @author acer01
 *  2018/7/28/028
 */
public class ChannelInvoker {

    private static final StringBuilder SESSION_COOKIE_1 = new StringBuilder(HttpConstants.JSESSION_ID_COOKIE + "=");
    private static final StringBuilder SESSION_COOKIE_2 = new StringBuilder(";" + HttpHeaderConstants.PATH + "=/;" + HttpHeaderConstants.HTTPONLY);

    public void writeAndReleaseFlushAndIfNeedClose(HttpServletObject httpServletObject, ByteBuf content, ChannelFutureListener[] finishListeners) {
        ChannelHandlerContext context = httpServletObject.getChannelHandlerContext();
        ServletHttpServletRequest servletRequest = httpServletObject.getHttpServletRequest();
        ServletHttpServletResponse servletResponse = httpServletObject.getHttpServletResponse();
        NettyHttpRequest nettyRequest = servletRequest.getNettyRequest();
        NettyHttpResponse nettyResponse = servletResponse.getNettyResponse();
        ServletSessionCookieConfig sessionCookieConfig = httpServletObject.getServletContext().getSessionCookieConfig();

        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(nettyRequest);

        settingResponse(isKeepAlive,content.readableBytes(),nettyResponse,servletRequest,servletResponse,sessionCookieConfig);
        writeResponse(isKeepAlive,context,nettyResponse,content,finishListeners);
    }

    private void writeResponse(boolean isKeepAlive,ChannelHandlerContext context,NettyHttpResponse nettyResponse,ByteBuf content,ChannelFutureListener[] finishListeners) {
        HttpContent httpContent = new DefaultLastHttpContent(content);

        ChannelFutureListener flushListener = null;
        if(finishListeners != null && finishListeners.length > 0) {
            flushListener = ChannelFutureFlushListener.newInstance(isKeepAlive,finishListeners);
        }

        context.write(nettyResponse, context.voidPromise());
        ChannelFuture flushChannelFuture = context.writeAndFlush(httpContent);
        if(flushListener != null) {
            flushChannelFuture.addListener(flushListener);
        }
    }

    /**
     * 设置基本的请求头
     * @param isKeepAlive 保持连接
     * @param totalLength 总内容长度
     * @param nettyResponse netty响应
     * @param servletRequest servlet请求
     * @param servletResponse servlet响应
     * @param sessionCookieConfig sessionCookie配置
     */
    private void settingResponse(boolean isKeepAlive, int totalLength, NettyHttpResponse nettyResponse,
                                ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse,
                                 ServletSessionCookieConfig sessionCookieConfig) {
        HttpHeaderUtil.setKeepAlive(nettyResponse, isKeepAlive);

        if (isKeepAlive && !HttpHeaderUtil.isContentLengthSet(nettyResponse)) {
            HttpHeaderUtil.setContentLength(nettyResponse, totalLength);
        }

        String contentType = servletResponse.getContentType();
        String characterEncoding = servletResponse.getCharacterEncoding();
        List<Cookie> cookies = servletResponse.getCookies();

        HttpHeaders headers = nettyResponse.headers();
        if (null != contentType) {
            //Content Type 响应头的内容
            String value = (null == characterEncoding) ? contentType :
                    new StringBuilder(contentType)
                            .append(';')
                            .append(HttpHeaderConstants.CHARSET)
                            .append('=')
                            .append(characterEncoding).toString();

            headers.set(HttpHeaderConstants.CONTENT_TYPE, value);
        }
        // 时间日期响应头
        headers.set(HttpHeaderConstants.DATE, ServletUtil.newDateGMT());
        //服务器信息响应头
        headers.set(HttpHeaderConstants.SERVER, servletRequest.getServletContext().getServerInfo());

        // cookies处理
        //long curTime = System.currentTimeMillis(); //用于根据maxAge计算Cookie的Expires
        //先处理Session ，如果是新Session 或 session失效 需要通过Cookie写入
        ServletHttpSession httpSession = servletRequest.getSession(true);
        if (httpSession.isNew()) {
            String sessionCookieName = sessionCookieConfig.getName();
            if(StringUtil.isEmpty(sessionCookieName)){
                sessionCookieName = HttpConstants.JSESSION_ID_COOKIE;
            }
            Cookie cookie = new Cookie(sessionCookieName,servletRequest.getRequestedSessionId());
            cookie.setHttpOnly(true);
            if(sessionCookieConfig.getDomain() != null) {
                cookie.setDomain(sessionCookieConfig.getDomain());
            }
            if(sessionCookieConfig.getPath() == null) {
                cookie.setPath("/");
            }else {
                cookie.setPath(sessionCookieConfig.getPath());
            }
            cookie.setSecure(sessionCookieConfig.isSecure());
            if(sessionCookieConfig.getComment() != null) {
                cookie.setComment(sessionCookieConfig.getComment());
            }
            if(cookies == null) {
                cookies = RecyclableUtil.newRecyclableList(1);
                cookies.add(cookie);
            }
//            String cookieStr = new StringBuilder(SESSION_COOKIE_1).append(servletRequest.getRequestedSessionId()).append(SESSION_COOKIE_2).toString();
//            headers.add(HttpHeaderConstants.SET_COOKIE, cookieStr);
        }

        //其他业务或框架设置的cookie，逐条写入到响应头去
        if(cookies != null) {
            NettyHttpCookie nettyCookie = new NettyHttpCookie();
            for (Cookie cookie : cookies) {
                nettyCookie.wrap(ServletUtil.toNettyCookie(cookie));
                headers.add(HttpHeaderConstants.SET_COOKIE, ServletUtil.encodeCookie(nettyCookie));
            }
        }
    }

    /**
     * 优化lambda实例数量, 减少gc次数
     */
    private static class ChannelFutureFlushListener implements ChannelFutureListener,Recyclable{
        private boolean isKeepAlive;
        private ChannelFutureListener[] finishListeners;

        private static final AbstractRecycler<ChannelFutureFlushListener> RECYCLER = new AbstractRecycler<ChannelFutureFlushListener>() {
            @Override
            protected ChannelFutureFlushListener newInstance() {
                return new ChannelFutureFlushListener();
            }
        };

        private static ChannelFutureFlushListener newInstance(boolean isKeepAlive,ChannelFutureListener[] finishListeners) {
            ChannelFutureFlushListener instance = RECYCLER.get();
            instance.isKeepAlive = isKeepAlive;
            instance.finishListeners = finishListeners;
            return instance;
        }

        @Override
        public void recycle() {
            isKeepAlive = false;
            finishListeners = null;
            RECYCLER.recycle(ChannelFutureFlushListener.this);
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            try {
                if(isKeepAlive){
                    for(ChannelFutureListener listener : finishListeners){
                        listener.operationComplete(future);
                    }
                }else {
                    ChannelFuture channelFuture = future.channel().close();
                    channelFuture.addListeners(finishListeners);
                }
            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }finally {
                ChannelFutureFlushListener.this.recycle();
            }
        }

    }

}
