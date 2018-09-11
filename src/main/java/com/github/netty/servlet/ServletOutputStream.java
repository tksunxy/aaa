package com.github.netty.servlet;

import com.github.netty.ContainerConfig;
import com.github.netty.core.NettyHttpCookie;
import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.CompositeByteBufX;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.util.*;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.servlet.util.ServletUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaders;

import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * servlet 输出流
 *
 * 频繁更改, 需要cpu对齐. 防止伪共享, 需设置 : -XX:-RestrictContended
 * @author 84215
 */
@sun.misc.Contended
public class ServletOutputStream extends javax.servlet.ServletOutputStream {

    private AtomicBoolean closed = new AtomicBoolean(false);
    private WriteListener writeListener;
    private HttpServletObject httpServletObject;
    private CompositeByteBufX content;
    private ContainerConfig config;

    ServletOutputStream() {}

    public void resetOutputTarget(HttpServletObject httpServletObject) {
        if(httpServletObject == null){
            this.httpServletObject = null;
            this.config = null;
            this.content = null;
            this.closed.set(true);
        }else {
            this.httpServletObject = httpServletObject;
            this.config = httpServletObject.getConfig();
            this.content = new CompositeByteBufX();
            this.closed.set(false);
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        Objects.requireNonNull(writeListener);
        //只能设置一次
        if(this.writeListener != null){
            return;
        }
        this.writeListener = writeListener;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if(len == 0){
            return;
        }

        int maxHeapByteLength = config.getResponseWriterChunkMaxHeapByteLength();
        ByteBuf ioByteBuf;
        if(len > maxHeapByteLength){
            ioByteBuf = content.alloc().directBuffer(len);
        }else {
            ioByteBuf = content.alloc().heapBuffer(len);
        }

        ioByteBuf.writeBytes(b,off,len);
        content.addComponent(ioByteBuf);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b,0,b.length);
    }

    @Override
    public void write(int b) throws IOException {
        int byteLen = 4;
        byte[] bytes = new byte[byteLen];
        IOUtil.setInt(bytes,0,b);
        write(bytes,0,byteLen);
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
    }

    @Override
    public void close() throws IOException {
        close(null);
    }

    /**
     * 结束响应对象
     * 当响应被关闭时，容器必须立即刷出响应缓冲区中的所有剩余的内容到客户端。
     * 以下事件表明servlet满足了请求且响应对象即将关闭：
     * ■servlet的service方法终止。
     * ■响应的setContentLength或setContentLengthLong方法指定了大于零的内容量，且已经写入到响应。
     * ■sendError 方法已调用。
     * ■sendRedirect 方法已调用。
     * ■AsyncContext 的complete 方法已调用
     * @throws IOException
     */
    public void close(ChannelFutureListener finishListener) throws IOException {
        if (closed.compareAndSet(false,true)) {
            try {
                content.writerIndex(content.capacity());

                //写入管道, 然后发送, 同时释放数据资源, 然后如果需要关闭则管理链接, 最后回调完成
                writeAndReleaseFlushAndIfNeedClose(httpServletObject, content, finishListener);
            }catch(Throwable e){
                ExceptionUtil.printRootCauseStackTrace(e);
                errorEvent(e);
            }
        }else {
            try {
                finishListener.operationComplete(null);
            } catch (Throwable e) {
                ExceptionUtil.printRootCauseStackTrace(e);
            }
        }
    }

    private void errorEvent(Throwable throwable){
        if(writeListener != null){
            writeListener.onError(throwable);
        }
    }

    private void checkClosed() throws ClosedChannelException {
        if(closed.get()){
            throw new ClosedChannelException();
        }
    }

    /**
     * 写入管道, 然后发送, 同时释放数据资源, 然后如果需要关闭则管理链接
     * @param httpServletObject
     * @param content
     * @param finishListener
     */
    private void writeAndReleaseFlushAndIfNeedClose(HttpServletObject httpServletObject, ByteBuf content, ChannelFutureListener finishListener) {
        ChannelHandlerContext context = httpServletObject.getChannelHandlerContext();
        ServletHttpServletRequest servletRequest = httpServletObject.getHttpServletRequest();
        ServletHttpServletResponse servletResponse = httpServletObject.getHttpServletResponse();
        NettyHttpRequest nettyRequest = servletRequest.getNettyRequest();
        NettyHttpResponse nettyResponse = servletResponse.getNettyResponse();
        ServletSessionCookieConfig sessionCookieConfig = httpServletObject.getServletContext().getSessionCookieConfig();

        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(nettyRequest);

        settingResponse(isKeepAlive,content.readableBytes(),nettyResponse,servletRequest,servletResponse,sessionCookieConfig);
        writeResponse(isKeepAlive,context,nettyResponse,content,finishListener);
    }

    /**
     * 写入响应
     * @param isKeepAlive
     * @param context
     * @param nettyResponse
     * @param content
     * @param finishListener
     */
    private void writeResponse(boolean isKeepAlive,ChannelHandlerContext context,NettyHttpResponse nettyResponse,ByteBuf content,ChannelFutureListener finishListener) {
        ChannelPromise promise;
        //如果需要保持链接 并且不需要回调
        if(isKeepAlive && finishListener == null) {
            promise = context.voidPromise();
        }else {
            promise = context.newPromise();
            promise.addListener(FlushListener.newInstance(isKeepAlive, finishListener));
        }

        nettyResponse.setContent(content);
        context.writeAndFlush(nettyResponse,promise);
    }

    /**
     * 设置响应头
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
    private static class FlushListener implements ChannelFutureListener,Recyclable {
        private boolean isKeepAlive;
        private ChannelFutureListener finishListener;

        private static final AbstractRecycler<FlushListener> RECYCLER = new AbstractRecycler<FlushListener>() {
            @Override
            protected FlushListener newInstance() {
                return new FlushListener();
            }
        };

        private static FlushListener newInstance(boolean isKeepAlive, ChannelFutureListener finishListener) {
            FlushListener instance = RECYCLER.get();
            instance.isKeepAlive = isKeepAlive;
            instance.finishListener = finishListener;
            return instance;
        }

        @Override
        public void recycle() {
            finishListener = null;
            RECYCLER.recycle(FlushListener.this);
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            try {
                if(isKeepAlive){
                    if(finishListener != null) {
                        finishListener.operationComplete(future);
                    }
                }else {
                    ChannelFuture channelFuture = future.channel().close();
                    if(finishListener != null) {
                        channelFuture.addListener(finishListener);
                    }
                }
            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }finally {
                FlushListener.this.recycle();
            }
        }

    }
}
