package com.github.netty.servlet;

import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.Recyclable;
import com.github.netty.util.TodoOptimize;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class ServletRequestDispatcher implements RequestDispatcher,Recyclable {

    FilterChain filterChain;

    private static final AbstractRecycler<ServletRequestDispatcher> RECYCLER = new AbstractRecycler<ServletRequestDispatcher>() {
        @Override
        protected ServletRequestDispatcher newInstance() {
            return new ServletRequestDispatcher();
        }
    };

    private ServletRequestDispatcher() {}

    public static ServletRequestDispatcher newInstance(FilterChain filterChain) {
        ServletRequestDispatcher instance = RECYCLER.get();
        instance.filterChain = filterChain;
        return instance;
    }

    @TodoOptimize("未实现forward方法")
    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if(request instanceof HttpServletRequest) {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            httpRequest.setAttribute(FORWARD_CONTEXT_PATH, httpRequest.getContextPath());
            httpRequest.setAttribute(FORWARD_PATH_INFO, httpRequest.getPathInfo());
            httpRequest.setAttribute(FORWARD_QUERY_STRING, httpRequest.getQueryString());
            httpRequest.setAttribute(FORWARD_REQUEST_URI, httpRequest.getRequestURI());
            httpRequest.setAttribute(FORWARD_SERVLET_PATH, httpRequest.getServletPath());
        }

        throw new ServletException("forward方法未实现");
    }

    @TodoOptimize("未实现include方法")
    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute(ServletHttpServletRequest.DISPATCHER_TYPE, DispatcherType.INCLUDE);

        if(request instanceof HttpServletRequest) {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            httpRequest.setAttribute(INCLUDE_CONTEXT_PATH, httpRequest.getContextPath());
            httpRequest.setAttribute(INCLUDE_PATH_INFO, httpRequest.getPathInfo());
            httpRequest.setAttribute(INCLUDE_QUERY_STRING, httpRequest.getQueryString());
            httpRequest.setAttribute(INCLUDE_REQUEST_URI, httpRequest.getRequestURI());
            httpRequest.setAttribute(INCLUDE_SERVLET_PATH, httpRequest.getServletPath());
        }
        throw new ServletException("include方法未实现");
    }

    public void dispatch(ServletRequest request, ServletResponse response,DispatcherType dispatcherType) throws ServletException, IOException {
        try {
            request.setAttribute(ServletHttpServletRequest.DISPATCHER_TYPE, dispatcherType);
            filterChain.doFilter(request, response);
        }finally {
            recycle();
        }
    }

    @Override
    public void recycle() {
        filterChain = null;
        RECYCLER.recycle(this);
    }

}
