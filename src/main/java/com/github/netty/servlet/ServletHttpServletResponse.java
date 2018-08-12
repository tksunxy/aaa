package com.github.netty.servlet;

import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.CompositeByteBufX;
import com.github.netty.core.support.Recyclable;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.servlet.support.MediaType;
import com.github.netty.util.HttpHeaderUtil;
import com.github.netty.util.ProxyUtil;
import com.github.netty.util.TodoOptimize;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletHttpServletResponse implements javax.servlet.http.HttpServletResponse,Recyclable {

    private static final AbstractRecycler<ServletHttpServletResponse> RECYCLER = new AbstractRecycler<ServletHttpServletResponse>() {
        @Override
        protected ServletHttpServletResponse newInstance() {
            if(ProxyUtil.isEnableProxy()){
                return ProxyUtil.newProxyByCglib(
                        ServletHttpServletResponse.class
                );
            }else {
                return new ServletHttpServletResponse();
            }
        }
    };

    private HttpServletObject httpServletObject;
    private NettyHttpResponse nettyResponse;

    private PrintWriter writer;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding;
    private Locale locale;
    private HttpHeaders nettyHeaders;

    private ServletOutputStream outputStream = new ServletOutputStream();
    private boolean commit = false;

    protected ServletHttpServletResponse() {}

    public static ServletHttpServletResponse newInstance(HttpServletObject httpServletObject) {
        Objects.requireNonNull(httpServletObject);

        ServletHttpServletResponse instance = RECYCLER.get();

        //Netty自带的http响应对象，初始化为200
        NettyHttpResponse nettyResponse = NettyHttpResponse.newInstance(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false));
        instance.nettyResponse = nettyResponse;
        instance.nettyHeaders = nettyResponse.headers();
        instance.httpServletObject = httpServletObject;
        //常用最大字节数 4096 * 6 = 24576字节
        instance.outputStream.wrap(new CompositeByteBufX(false,6));
        instance.outputStream.setHttpServletObject(httpServletObject);
        return instance;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public NettyHttpResponse getNettyResponse() {
        return nettyResponse;
    }

    private void checkNotCommitted() {
        if(commit) {
            throw new IllegalStateException("Cannot perform this operation after response has been committed");
        }
    }

    private boolean setHeaderField(String name, String value) {
        char c = name.charAt(0);//减少判断的时间，提高效率
        if ('C' == c || 'c' == c) {
            if (HttpHeaderConstants.CONTENT_TYPE.toString().equalsIgnoreCase(name)) {
                setContentType(value);
                return true;
            }
        }
        return false;
    }

    @Override
    public void addCookie(Cookie cookie) {
        if(cookies == null){
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return nettyHeaders.contains((CharSequence) name);
    }

    @Override
    public String encodeURL(String url) {
        if(!httpServletObject.getHttpServletRequest().isRequestedSessionIdFromCookie()){
            //来自Cookie的Session ID,则客户端肯定支持Cookie，无需重写URL
            return url;
        }
        return url + ";" + HttpConstants.JSESSION_ID_PARAMS + "=" + httpServletObject.getHttpServletRequest().getRequestedSessionId();
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        checkNotCommitted();
        nettyResponse.setStatus(new HttpResponseStatus(sc, msg));
        commit = true;
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkNotCommitted();
        nettyResponse.setStatus(HttpResponseStatus.valueOf(sc));
        commit = true;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkNotCommitted();
        nettyResponse.setStatus(HttpResponseStatus.FOUND);
        nettyHeaders.set(HttpHeaderConstants.LOCATION, (CharSequence)location);
        commit = true;
    }

    @Override
    public void setDateHeader(String name, long date) {
        nettyHeaders.set((CharSequence) name,(CharSequence) String.valueOf(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        if (commit) {
            return;
        }
        nettyHeaders.add((CharSequence)name, (CharSequence)String.valueOf(date));
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (commit) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        nettyHeaders.set((CharSequence)name, (CharSequence)value);
    }

    @Override
    public void addHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (commit) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        nettyHeaders.add((CharSequence)name, (CharSequence)value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (commit) {
            return;
        }
        nettyHeaders.set((CharSequence)name, (CharSequence)String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (commit) {
            return;
        }
        nettyHeaders.add((CharSequence) name, (CharSequence) String.valueOf(value));
    }

    @Override
    public void setContentType(String type) {
        if (type == null) {
            contentType = null;
            return;
        }

        MediaType mediaType = MediaType.parseFast(type);
        contentType = mediaType.toStringNoCharset();
        String charset = mediaType.getCharset();
        if (charset != null) {
            setCharacterEncoding(charset);
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setStatus(int sc) {
        nettyResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        nettyResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public int getStatus() {
        return nettyResponse.getStatus().code();
    }

    @Override
    public String getHeader(String name) {
        Object value = nettyHeaders.get((CharSequence) name);
        return value == null? null : String.valueOf(value);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List list = nettyHeaders.getAll((CharSequence) name);
        List<String> stringList = new LinkedList<>();
        for(Object charSequence : list){
            stringList.add(String.valueOf(charSequence));
        }
        return stringList;
    }

    @Override
    public Collection<String> getHeaderNames() {
        Set nameSet = nettyHeaders.names();

        List<String> nameList = new LinkedList<>();
        for(Object charSequence : nameSet){
            nameList.add(String.valueOf(charSequence));
        }
        return nameList;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    //Writer和OutputStream不能同时使用
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        writer = new PrintWriter(outputStream);
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (null != writer) {
            return;
        }
        characterEncoding = charset;
    }

    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaderUtil.setContentLength(nettyResponse, len);

        if(len > 0 && outputStream.getContentLength() > 0){
            commit = true;
        }
    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        outputStream.flush();
    }

    @Override
    public void resetBuffer() {
        //
    }

    @Override
    public boolean isCommitted() {
        return commit;
    }

    @TodoOptimize("重置流")
    @Override
    public void reset() {
        resetBuffer();
        writer = null;
    }

    @Override
    public void setLocale(Locale loc) {
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return null == locale ? Locale.getDefault() : locale;
    }

    @Override
    public void recycle() {
        try {
            outputStream.close(ChannelFutureCloseListener.newInstance(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 优化lambda实例数量, 减少gc次数
     */
    static class ChannelFutureCloseListener implements ChannelFutureListener,Recyclable{
        private ServletHttpServletResponse closeTarget;

        private static final AbstractRecycler<ChannelFutureCloseListener> RECYCLER = new AbstractRecycler<ChannelFutureCloseListener>() {
            @Override
            protected ChannelFutureCloseListener newInstance() {
                return new ChannelFutureCloseListener();
            }
        };

        public static ChannelFutureCloseListener newInstance(ServletHttpServletResponse closeTarget) {
            ChannelFutureCloseListener instance = RECYCLER.get();
            instance.closeTarget = closeTarget;
            return instance;
        }

        @Override
        public void recycle() {
            closeTarget = null;
            ChannelFutureCloseListener.RECYCLER.recycle(ChannelFutureCloseListener.this);
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            closeTarget.nettyResponse.recycle();

            closeTarget.outputStream.setHttpServletObject(null);
            closeTarget.httpServletObject = null;
            closeTarget.nettyResponse = null;
            closeTarget.writer = null;
            closeTarget.cookies = null;
            closeTarget.contentType = null;
            closeTarget.characterEncoding = null;
            closeTarget.locale = null;
            closeTarget.nettyHeaders = null;
            closeTarget.commit = false;

            ServletHttpServletResponse.RECYCLER.recycle(closeTarget);
            ChannelFutureCloseListener.this.recycle();
        }
    }
}
