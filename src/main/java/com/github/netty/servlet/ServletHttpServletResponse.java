package com.github.netty.servlet;

import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.servlet.support.ChannelInvoker;
import com.github.netty.servlet.support.MediaType;
import com.github.netty.util.HttpHeaderUtil;
import com.github.netty.util.ProxyUtil;
import com.github.netty.util.TodoOptimize;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
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
        protected ServletHttpServletResponse newInstance(Handle<ServletHttpServletResponse> handle) {
            if(ProxyUtil.isEnableProxy()){
                return ProxyUtil.newProxyByCglib(
                        ServletHttpServletResponse.class,
                        new Class[]{Handle.class},
                        new Object[]{handle});
            }else {
                return new ServletHttpServletResponse(handle);
            }
        }
    };
    private final AbstractRecycler.Handle<ServletHttpServletResponse> handle;

    private ServletHttpServletRequest httpServletRequest;
    private NettyHttpResponse nettyResponse;
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding;
    private Locale locale;
    private HttpHeaders nettyHeaders;

    protected ServletHttpServletResponse(AbstractRecycler.Handle<ServletHttpServletResponse> handle) {
        this.handle = handle;
        this.outputStream = new ServletOutputStream();
    }

    public static ServletHttpServletResponse newInstance(ChannelHandlerContext ctx, ServletHttpServletRequest httpServletRequest) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(httpServletRequest);

        ServletHttpServletResponse instance = RECYCLER.get();

        //Netty自带的http响应对象，初始化为200
        NettyHttpResponse nettyResponse = NettyHttpResponse.newInstance(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false));
        instance.nettyHeaders = nettyResponse.headers();
        instance.nettyResponse = nettyResponse;
        instance.httpServletRequest = httpServletRequest;
        instance.characterEncoding = null;
        instance.outputStream.wrap(new CompositeByteBuf(ctx.alloc(),false,16));
        instance.outputStream.setChannelInvoker(new ChannelInvoker(ctx,httpServletRequest,instance));
        return instance;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public NettyHttpResponse getNettyResponse() {
        return nettyResponse;
    }

    private void checkNotCommitted() {
        checkState(isCommitted(), "Cannot perform this operation after response has been committed");
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
        if(!httpServletRequest.isRequestedSessionIdFromCookie()){
            //来自Cookie的Session ID,则客户端肯定支持Cookie，无需重写URL
            return url;
        }
        return url + ";" + HttpConstants.JSESSION_ID_PARAMS + "=" + httpServletRequest.getRequestedSessionId();
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
        outputStream.close();
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkNotCommitted();
        nettyResponse.setStatus(HttpResponseStatus.valueOf(sc));
        outputStream.close();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkNotCommitted();
        nettyResponse.setStatus(HttpResponseStatus.FOUND);
        nettyHeaders.set(HttpHeaderConstants.LOCATION, (CharSequence)location);
        outputStream.close();
    }

    @Override
    public void setDateHeader(String name, long date) {
        nettyHeaders.set((CharSequence) name,(CharSequence) String.valueOf(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        nettyHeaders.add((CharSequence)name, (CharSequence)String.valueOf(date));
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
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
        if (isCommitted()) {
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
        if (isCommitted()) {
            return;
        }
        nettyHeaders.set((CharSequence)name, (CharSequence)String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
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
        if (hasWriter()) {
            return;
        }
        characterEncoding = charset;
    }

    private boolean hasWriter() {
        return null != writer;
    }

    private void checkState(boolean isTrue, String errorMessage) {
        if(isTrue) {
            throw new IllegalStateException(errorMessage);
        }
    }

    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaderUtil.setContentLength(nettyResponse, len);

        if(len > 0 && outputStream.getContentLength() > 0){
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        return outputStream.isClosed();
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
        ServletAsyncContext asyncContext = httpServletRequest.getAsyncContext();
        boolean isAsync = asyncContext != null && asyncContext.isStarted();

        /*
         * 每个响应对象是只有当在servlet的service方法的范围内或在filter的doFilter方法范围内是有效的，除非该
         * 组件关联的请求对象已经开启异步处理。如果相关的请求已经启动异步处理，那么直到AsyncContext的
         * complete 方法被调用，请求对象一直有效。为了避免响应对象创建的性能开销，容器通常回收响应对象。
         * 在相关的请求的startAsync 还没有调用时，开发人员必须意识到保持到响应对象引用，超出之上描述的范
         * 围可能导致不确定的行为
         */
        NettyHttpResponse nettyResponseTemp = nettyResponse;

        //如果是异步, 或者已经关闭
        if (isAsync || outputStream.isClosed()) {
            nettyResponseTemp.recycle();
        }else {
            try {
                outputStream.close(future -> {
                    nettyResponseTemp.recycle();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        httpServletRequest = null;
        nettyResponse = null;
        writer = null;
        cookies = null;
        contentType = null;
        characterEncoding = null;
        locale = null;
        nettyHeaders = null;
        handle.recycle(this);
    }

}
