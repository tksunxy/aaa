package com.github.netty.servlet.support;

import com.github.netty.core.NettyHttpCookie;
import com.github.netty.core.NettyHttpRequest;
import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.util.HttpHeaderUtil;
import com.github.netty.util.ServletUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.http.Cookie;
import java.util.List;
import java.util.StringJoiner;

/**
 *
 * @author acer01
 *  2018/7/28/028
 */
public class ServletOutputStreamListener implements StreamListener {

    private ServletHttpServletRequest servletRequest;
    private ServletHttpServletResponse servletResponse;

    public ServletOutputStreamListener(ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    @Override
    public void closeBefore(int totalLength) {
        NettyHttpRequest nettyRequest = servletRequest.getNettyRequest();
        NettyHttpResponse nettyResponse = servletResponse.getNettyResponse();

        settingResponse(nettyRequest,nettyResponse,servletRequest,servletResponse,totalLength);
    }

    @Override
    public void closeAfter(ByteBuf content) {
        if(content.refCnt() > 0) {
            ReferenceCountUtil.safeRelease(content);
        }
    }

    /**
     * 设置基本的请求头
     * @param nettyRequest netty请求
     * @param nettyResponse netty响应
     * @param servletRequest servlet请求
     * @param servletResponse servlet响应
     * @param totalLength 总内容长度
     */
    public void settingResponse(NettyHttpRequest nettyRequest, NettyHttpResponse nettyResponse,
                                ServletHttpServletRequest servletRequest, ServletHttpServletResponse servletResponse, int totalLength) {
        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(nettyRequest);
        HttpHeaderUtil.setKeepAlive(nettyResponse, isKeepAlive);

        if (!isKeepAlive && !HttpHeaderUtil.isContentLengthSet(nettyResponse)) {
            HttpHeaderUtil.setContentLength(nettyResponse, totalLength);
        }

        String contentType = servletResponse.getContentType();
        String characterEncoding = servletResponse.getCharacterEncoding();
        List<Cookie> cookies = servletResponse.getCookies();

        HttpHeaders headers = nettyResponse.headers();
        if (null != contentType) {
            String value = (null == characterEncoding) ? contentType : contentType + "; "+HttpHeaderConstants.CHARSET+"=" + characterEncoding; //Content Type 响应头的内容
            headers.set(HttpHeaderConstants.CONTENT_TYPE, value);
        }
        headers.set(HttpHeaderConstants.DATE, ServletUtil.newDateGMT()); // 时间日期响应头
        headers.set(HttpHeaderConstants.SERVER, servletRequest.getServletContext().getServerInfo()); //服务器信息响应头

        // cookies处理
        //long curTime = System.currentTimeMillis(); //用于根据maxAge计算Cookie的Expires
        //先处理Session ，如果是新Session需要通过Cookie写入
        if (servletRequest.getSession().isNew()) {
            StringJoiner cookieStrJoiner = new StringJoiner("; ");
            cookieStrJoiner.add(HttpConstants.JSESSION_ID_COOKIE + "=" + servletRequest.getRequestedSessionId());
            cookieStrJoiner.add("path=/");
            cookieStrJoiner.add("secure");
            cookieStrJoiner.add("HttpOnly");

            String serverName = servletRequest.getServerName();
            if(!ServletUtil.isLocalhost(serverName)){
                cookieStrJoiner.add("domain=" + serverName);
            }

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
