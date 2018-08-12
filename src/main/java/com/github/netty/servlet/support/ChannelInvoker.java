package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpCookie;
import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletHttpSession;
import com.github.netty.util.ExceptionUtil;
import com.github.netty.util.HttpHeaderUtil;
import com.github.netty.util.ServletUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;

import javax.servlet.http.Cookie;
import java.util.List;
import java.util.StringJoiner;

/**
 *
 * @author acer01
 *  2018/7/28/028
 */
public class ChannelInvoker {

    public void writeAndReleaseFlushAndIfNeedClose(HttpServletObject httpServletObject, ByteBuf content, ChannelFutureListener[] finishListeners) {
        ChannelHandlerContext context = httpServletObject.getChannelHandlerContext();
        ServletHttpServletRequest servletRequest = httpServletObject.getHttpServletRequest();
        ServletHttpServletResponse servletResponse = httpServletObject.getHttpServletResponse();
        NettyHttpRequest nettyRequest = servletRequest.getNettyRequest();
        NettyHttpResponse nettyResponse = servletResponse.getNettyResponse();

        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(nettyRequest);

        settingResponse(isKeepAlive,content.readableBytes(),nettyResponse,servletRequest,servletResponse);
        writeResponse(isKeepAlive,context,nettyResponse,content,finishListeners);
    }

    private void writeResponse(boolean isKeepAlive,ChannelHandlerContext context,NettyHttpResponse nettyResponse,ByteBuf content,ChannelFutureListener[] finishListeners) {
        HttpContent httpContent = buildContent(content, isKeepAlive);
        ChannelFutureListener flushListener = newFlushListener(isKeepAlive,finishListeners);

        context.write(nettyResponse, context.voidPromise());
        context.writeAndFlush(httpContent).addListener(flushListener);
    }

    private ChannelFutureListener newFlushListener(boolean isKeepAlive,ChannelFutureListener[] finishListeners){
        ChannelFutureListener flushListener = future -> {
            try {
                if(finishListeners != null && finishListeners.length > 0) {
                    if(isKeepAlive){
                        for(ChannelFutureListener listener : finishListeners){
                            listener.operationComplete(future);
                        }
                    }else {
                        ChannelFuture channelFuture = future.channel().close();
                        channelFuture.addListeners(finishListeners);
                    }
                }
            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }
        };
        return flushListener;
    }

    private HttpContent buildContent(ByteBuf byteBuf,boolean isKeepAlive){
        HttpContent httpContent;
        if(isKeepAlive){
            httpContent = new DefaultLastHttpContent(byteBuf);
        }else {
            httpContent = new DefaultLastHttpContent(byteBuf);
        }
        return httpContent;
    }

    /**
     * 设置基本的请求头
     * @param isKeepAlive 保持连接
     * @param totalLength 总内容长度
     * @param nettyResponse netty响应
     * @param servletRequest servlet请求
     * @param servletResponse servlet响应
     */
    private void settingResponse(boolean isKeepAlive, int totalLength, NettyHttpResponse nettyResponse,
                                ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse) {
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
            String value = (null == characterEncoding) ? contentType : contentType + "; "+HttpHeaderConstants.CHARSET+"=" + characterEncoding;
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
            StringJoiner cookieStrJoiner = new StringJoiner(";");
            cookieStrJoiner.add(HttpConstants.JSESSION_ID_COOKIE + "=" + servletRequest.getRequestedSessionId());
            cookieStrJoiner.add(HttpHeaderConstants.PATH + "=/");
            cookieStrJoiner.add(HttpHeaderConstants.HTTPONLY);
//            cookieStrJoiner.add("Secure");
//            cookieStrJoiner.add("Expires=-1");
//
//            String serverName = servletRequest.getServerName();
//            int port = servletRequest.getServerPort();
//            if(!ServletUtil.isLocalhost(serverName)){
//                if (port != HttpConstants.HTTP_PORT && port != HttpConstants.HTTPS_PORT) {
//                    serverName+= ":" + port;
//                }
//                cookieStrJoiner.add("Domain=" + serverName);
//            }

            headers.add(HttpHeaderConstants.SET_COOKIE, cookieStrJoiner.toString());
        }

        //其他业务或框架设置的cookie，逐条写入到响应头去
        if(cookies != null) {
            NettyHttpCookie nettyCookie = new NettyHttpCookie();
            for (Cookie cookie : cookies) {
                nettyCookie.wrap(ServletUtil.toNettyCookie(cookie));
                if(cookie == null){
                    continue;
                }
                headers.add(HttpHeaderConstants.SET_COOKIE, ServletUtil.encodeCookie(nettyCookie));
            }
        }
    }

}
