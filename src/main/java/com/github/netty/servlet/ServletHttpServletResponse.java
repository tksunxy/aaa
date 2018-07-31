package com.github.netty.servlet;

import com.github.netty.core.adapter.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.servlet.support.ServletOutputStreamListener;
import com.github.netty.util.HttpHeaderUtil;
import com.github.netty.util.obj.TodoOptimize;
import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletHttpServletResponse implements javax.servlet.http.HttpServletResponse {

    private ServletHttpServletRequest httpServletRequest;
    private NettyHttpResponse nettyResponse;
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding;
    private Locale locale;
    private HttpHeaders nettyHeaders;
    
    /**
     * 构造方法
     * @param ctx            Netty的Context
     * @param httpServletRequest servlet请求
     */
    public ServletHttpServletResponse(ChannelHandlerContext ctx, ServletHttpServletRequest httpServletRequest) {
        //Netty自带的http响应对象，初始化为200
        this.nettyResponse = new NettyHttpResponse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false));
        this.nettyHeaders = nettyResponse.headers();
        this.outputStream = new ServletOutputStream(ctx, nettyResponse,HttpHeaderUtil.isKeepAlive(httpServletRequest.getNettyRequest()));
        outputStream.addStreamListener(new ServletOutputStreamListener(httpServletRequest,this));

        this.httpServletRequest = httpServletRequest;
        this.characterEncoding = null;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public NettyHttpResponse getNettyResponse() {
        return nettyResponse;
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
        return nettyHeaders.contains(name);
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
        nettyHeaders.set(HttpHeaderConstants.LOCATION, location);
        outputStream.close();
    }

    @Override
    public void setDateHeader(String name, long date) {
        nettyHeaders.set(name, String.valueOf(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        nettyHeaders.add(name, String.valueOf(date));
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
        nettyHeaders.set(name, value);
    }

    private boolean setHeaderField(String name, String value) {
        char c = name.charAt(0);//减少判断的时间，提高效率
        if ('C' == c || 'c' == c) {
            if (HttpHeaderConstants.CONTENT_TYPE.equalsIgnoreCase(name)) {
                setContentType(value);
                return true;
            }
        }
        return false;
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
        nettyHeaders.add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        nettyHeaders.set(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        nettyHeaders.add(name, String.valueOf(value));
    }

    @Override
    public void setContentType(String type) {
        if (isCommitted()) {
            return;
        }
        if (hasWriter()) {
            return;
        }
        if (null == type) {
            contentType = null;
            return;
        }
        MediaType mediaType = MediaType.parse(type);
        Optional<Charset> charset = mediaType.charset();
        if (charset.isPresent()) {
            setCharacterEncoding(charset.get().name());
        }
        contentType = mediaType.type() + '/' + mediaType.subtype();
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
        Object value = nettyHeaders.get(name);
        return value == null? null : String.valueOf(value);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List list = nettyHeaders.getAll(name);
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
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    @Override
    public void setContentLength(int len) {
        HttpHeaderUtil.setContentLength(nettyResponse, len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaderUtil.setContentLength(nettyResponse, len);
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

    private void checkNotCommitted() {
        checkState(isCommitted(), "Cannot perform this operation after response has been committed");
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
}
