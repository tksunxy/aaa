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
import com.github.netty.util.TodoOptimize;
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

    private ServletHttpServletRequest servletRequest;
    private ServletHttpServletResponse servletResponse;
    private NettyHttpRequest nettyRequest;
    private NettyHttpResponse nettyResponse;
    private ChannelHandlerContext ctx;
    @TodoOptimize("缺少对keep-alive的支持")
    private boolean isKeepAlive;
    
    public ChannelInvoker(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse) {
        this.ctx = ctx;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.nettyRequest = servletRequest.getNettyRequest();
        this.nettyResponse = servletResponse.getNettyResponse();
        this.isKeepAlive = HttpHeaderUtil.isKeepAlive(nettyRequest);
    }

    public void writeAndFlushAndIfNeedClose(ByteBuf content, ChannelFutureListener[] finishListeners) {
        settingResponse(nettyResponse,servletRequest,servletResponse,content.capacity());

        writeResponse(content,finishListeners);
        this.servletRequest = null;
        this.servletResponse = null;
        this.nettyRequest = null;
        this.nettyResponse = null;
    }

    private void writeResponse(ByteBuf content,ChannelFutureListener[] finishListeners) {
        HttpContent httpContent = buildContent(content, isKeepAlive);
        ChannelFutureListener flushListener = newFlushListener(finishListeners);
        
        ctx.write(nettyResponse, ctx.voidPromise());
        ctx.writeAndFlush(httpContent).addListener(flushListener);
    }

    private ChannelFutureListener newFlushListener(ChannelFutureListener[] finishListeners){
        ChannelFutureListener flushListener = new ChannelFutureListener() {
            /**
             * 写完后1.刷新 2.释放内存 3.关闭流
             * @param future 回调对象
             * @throws Exception 异常
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if(!isKeepAlive){
                        ChannelFuture channelFuture = future.channel().close();
                        if(finishListeners != null && finishListeners.length > 0) {
                            channelFuture.addListeners(finishListeners);
                        }
                    }

                }catch (Throwable throwable){
                    ExceptionUtil.printRootCauseStackTrace(throwable);
                }
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
     * @param nettyResponse netty响应
     * @param servletRequest servlet请求
     * @param servletResponse servlet响应
     * @param totalLength 总内容长度
     */
    private void settingResponse(NettyHttpResponse nettyResponse,
                                ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse, int totalLength) {
        HttpHeaderUtil.setKeepAlive(nettyResponse, isKeepAlive);

        if (!isKeepAlive && !HttpHeaderUtil.isContentLengthSet(nettyResponse)) {
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
            cookieStrJoiner.add("path=/");
//            cookieStrJoiner.add("secure");
//            cookieStrJoiner.add("HttpOnly");
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
