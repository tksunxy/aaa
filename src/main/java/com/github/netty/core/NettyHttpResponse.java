package com.github.netty.core;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Created by acer01 on 2018/7/28/028.
 */
public class NettyHttpResponse extends DefaultHttpResponse {

    public NettyHttpResponse(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
    }

    public NettyHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders) {
        super(version, status, validateHeaders);
    }

    public NettyHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleHeaderFields) {
        super(version, status, validateHeaders, singleHeaderFields);
    }

}
