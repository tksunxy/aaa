package com.github.netty.servlet.support;

import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;

/**
 * Created by acer01 on 2018/8/1/001.
 */
public class HttpServletObject {
    private ServletHttpServletRequest httpServletRequest;
    private ServletHttpServletResponse httpServletResponse;

    public HttpServletObject(ServletHttpServletRequest httpServletRequest, ServletHttpServletResponse httpServletResponse) {
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    public ServletHttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public ServletHttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }
}
