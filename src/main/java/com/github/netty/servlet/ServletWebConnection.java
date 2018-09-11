package com.github.netty.servlet;

import com.github.netty.core.util.TodoOptimize;
import com.github.netty.servlet.support.HttpServletObject;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.WebConnection;
import java.io.IOException;

/**
 * servlet 链接
 * @author 84215
 */
@TodoOptimize("协议升级未实现")
public class ServletWebConnection implements WebConnection {

    private HttpServletObject httpServletObject;

    public ServletWebConnection(HttpServletObject httpServletObject) {
        this.httpServletObject = httpServletObject;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return httpServletObject.getHttpServletRequest().getInputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return httpServletObject.getHttpServletResponse().getOutputStream();
    }

    @Override
    public void close() throws Exception {
        getInputStream().close();
        getOutputStream().close();
    }

}
