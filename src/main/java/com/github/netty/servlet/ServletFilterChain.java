package com.github.netty.servlet;

import com.github.netty.servlet.support.ServletEventListenerManager;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 84215
 */
public class ServletFilterChain implements FilterChain {

    /**
     * 考虑到每个请求只有一个线程处理，而且ServletContext在每次请求时都会new 一个SimpleFilterChain对象
     * 所以这里把过滤器链的Iterator作为FilterChain的私有变量，没有线程安全问题
     */
    private final List<Filter> filterList;
    private final Servlet servlet;
    private ServletContext servletContext;
    private int pos;

    public static Map<Filter,AtomicLong> FILTER_TIME_MAP = new ConcurrentHashMap<>();
    public static AtomicLong SERVLET_TIME = new AtomicLong();
    public static AtomicLong FILTER_TIME = new AtomicLong();
    long begin = System.currentTimeMillis();

    ServletFilterChain(ServletContext servletContext, Servlet servlet, List<Filter> filterList){
        this.servletContext = servletContext;
        this.filterList = filterList;
        this.servlet = servlet;
    }

    /**
     * 每个Filter在处理完请求之后调用FilterChain的这个方法。
     * 这时候应该找到下一个Filter，调用其doFilter()方法。
     * 如果没有下一个了，应该调用servlet的service()方法了
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();

        if(pos == 0){
            if(listenerManager.hasServletRequestListener()) {
                listenerManager.onServletRequestInitialized(new ServletRequestEvent(servletContext,request));
            }
        }

        if(pos < filterList.size()){
            Filter filter = filterList.get(pos);
            pos++;
            filter.doFilter(request, response, this);

            FILTER_TIME_MAP.put(filter,FILTER_TIME);
        }else {
            try {
                long c = System.currentTimeMillis();
                FILTER_TIME.addAndGet(c - begin);

                servlet.service(request, response);

                long end = System.currentTimeMillis() - c;
                SERVLET_TIME.addAndGet(end);
            }finally {
                if(listenerManager.hasServletRequestListener()) {
                    listenerManager.onServletRequestDestroyed(new ServletRequestEvent(servletContext,request));
                }
            }
        }

    }

}
