package com.github.netty.servlet.support;

import com.github.netty.servlet.ServletAsyncContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

import java.util.Objects;

/**
 *
 * @author acer01
 *  2018/8/1/001
 */
public class HttpServletObject extends AbstractReferenceCounted{

    private ServletHttpServletRequest httpServletRequest;
    private ServletHttpServletResponse httpServletResponse;

    public HttpServletObject() {
    }

    public HttpServletObject(ServletHttpServletRequest httpServletRequest, ServletHttpServletResponse httpServletResponse) {
        this.httpServletRequest = Objects.requireNonNull(httpServletRequest);
        this.httpServletResponse = Objects.requireNonNull(httpServletResponse);
    }

    public ServletHttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public ServletHttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    protected void deallocate() {
        if(httpServletRequest == null || httpServletResponse == null){
            return;
        }

        try {
            httpServletRequest.getInputStream().close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        try {
            /*
             * 每个响应对象是只有当在servlet的service方法的范围内或在filter的doFilter方法范围内是有效的，除非该
             * 组件关联的请求对象已经开启异步处理。如果相关的请求已经启动异步处理，那么直到AsyncContext的
             * complete 方法被调用，请求对象一直有效。为了避免响应对象创建的性能开销，容器通常回收响应对象。
             * 在相关的请求的startAsync 还没有调用时，开发人员必须意识到保持到响应对象引用，超出之上描述的范
             * 围可能导致不确定的行为
             */
            ServletAsyncContext asyncContext = httpServletRequest.getAsyncContext();
            if (asyncContext == null || !asyncContext.isStarted()) {
                httpServletResponse.getOutputStream().close();
            }
            httpServletRequest = null;
            httpServletResponse = null;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public ReferenceCounted touch(Object o) {
        return this;
    }

}
