package com.github.netty.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class ServletRequestDispatcher implements RequestDispatcher {

    private FilterChain filterChain;

    ServletRequestDispatcher(FilterChain filterChain) {
        this.filterChain = filterChain;
    }

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

        dispatch(request,response,DispatcherType.FORWARD);
        // TODO implement
    }

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

        // TODO implement
    }

    public void dispatch(ServletRequest request, ServletResponse response,DispatcherType dispatcherType) throws ServletException, IOException {
        request.setAttribute(ServletHttpServletRequest.DISPATCHER_TYPE, dispatcherType);
        filterChain.doFilter(request, response);
    }

}
