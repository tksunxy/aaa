package com.github.netty.servlet;

import com.github.netty.servlet.support.HttpServletObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletAsyncContext implements AsyncContext {

    private ServletRequest servletRequest;
    private ServletResponse servletResponse;
    private ExecutorService executorService;

    /**
     * 0=初始, 1=开始, 2=完成
      */
    private int status;
    private static final int STATUS_INIT = 0;
    private static final int STATUS_START = 1;
    private static final int STATUS_COMPLETE = 2;

    /**
     * 超时时间 -> 毫秒
     */
    private long timeout;

    private List<ServletAsyncListenerWrapper> asyncListenerWrapperList;

    private ServletContext servletContext;
    private HttpServletObject httpServletObject;
    private Throwable throwable;

    public ServletAsyncContext(HttpServletObject httpServletObject,ServletContext servletContext, ExecutorService executorService, ServletRequest servletRequest, ServletResponse servletResponse) {
        this.httpServletObject = Objects.requireNonNull(httpServletObject);
        this.servletContext = Objects.requireNonNull(servletContext);
        this.executorService = Objects.requireNonNull(executorService);
        this.servletRequest = Objects.requireNonNull(servletRequest);
        this.servletResponse = Objects.requireNonNull(servletResponse);
        this.status = STATUS_INIT;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public ServletRequest getRequest() {
        return servletRequest;
    }

    @Override
    public ServletResponse getResponse() {
        return servletResponse;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return true;
    }

    @Override
    public void dispatch() {
        if(servletRequest == null){
            return;
        }
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String path = request.getServletPath();
            String pathInfo = request.getPathInfo();
            if (null != pathInfo) {
                path += pathInfo;
            }
            dispatch(path);
        }
    }

    @Override
    public void dispatch(String path) {
        dispatch(servletContext, path);
    }

    @Override
    public void dispatch(javax.servlet.ServletContext context, String path) {
        check();

        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        if (httpRequest.getAttribute(ASYNC_REQUEST_URI)==null) {
            httpRequest.setAttribute(ASYNC_CONTEXT_PATH, httpRequest.getContextPath());
            httpRequest.setAttribute(ASYNC_PATH_INFO, httpRequest.getPathInfo());
            httpRequest.setAttribute(ASYNC_QUERY_STRING, httpRequest.getQueryString());
            httpRequest.setAttribute(ASYNC_REQUEST_URI, httpRequest.getRequestURI());
            httpRequest.setAttribute(ASYNC_SERVLET_PATH, httpRequest.getServletPath());
        }

        ServletContext servletContext = unWrapper(context);
        if(servletContext == null){
            servletContext = this.servletContext;
        }

        ServletRequestDispatcher dispatcher = servletContext.getRequestDispatcher(path);

        start(()->{
            try {
                dispatcher.dispatch(httpRequest, servletResponse,DispatcherType.ASYNC);
            } catch (Exception e) {
                throw new AsyncRuntimeException(e);
            }
        });

    }

    @Override
    public void complete() {
        status = STATUS_COMPLETE;
        httpServletObject.recycle();
    }

    @Override
    public void start(Runnable runnable) {
        status = STATUS_START;
        Runnable taskWrapper = newTaskWrapper(runnable);
//        taskWrapper.run();
        executorService.execute(taskWrapper);
    }

    private Runnable newTaskWrapper(Runnable run){
        return () -> {
            Future future = executorService.submit(run);
            try {
                //通知开始
                notifyEvent(listenerWrapper -> {
                    AsyncEvent event = new AsyncEvent(ServletAsyncContext.this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,null);
                    try {
                        listenerWrapper.asyncListener.onStartAsync(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                future.get(getTimeout(), TimeUnit.MILLISECONDS);

            } catch (TimeoutException e) {
                //通知超时
                notifyEvent(listenerWrapper -> {
                    try {
                        AsyncEvent event = new AsyncEvent(ServletAsyncContext.this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,null);
                        listenerWrapper.asyncListener.onTimeout(event);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }catch (Throwable throwable){
                if(throwable instanceof AsyncRuntimeException){
                    throwable = throwable.getCause();
                }
                setThrowable(throwable);

                //通知异常
                notifyEvent(listenerWrapper -> {
                    AsyncEvent event = new AsyncEvent(ServletAsyncContext.this,listenerWrapper.servletRequest,listenerWrapper.servletResponse, getThrowable());
                    try {
                        listenerWrapper.asyncListener.onError(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }finally {
                //通知结束
                notifyEvent(listenerWrapper -> {
                    try {
                        AsyncEvent event = new AsyncEvent(ServletAsyncContext.this,listenerWrapper.servletRequest,listenerWrapper.servletResponse, getThrowable());
                        listenerWrapper.asyncListener.onComplete(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                complete();
            }
        };
    }

    @Override
    public void addListener(AsyncListener listener) {
        addListener(listener,servletRequest,servletResponse);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        if(asyncListenerWrapperList == null){
            asyncListenerWrapperList = new LinkedList<>();
        }

        asyncListenerWrapperList.add(new ServletAsyncListenerWrapper(listener,servletRequest,servletResponse));
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    private void check() {
        if (servletRequest == null) {
            // AsyncContext has been recycled and should not be being used
            throw new IllegalStateException("请求不能为空");
        }
    }

    private ServletContext unWrapper(javax.servlet.ServletContext context){
        return (ServletContext) context;
    }

    public boolean isStarted(){
        return status >= STATUS_START;
    }

    private void notifyEvent(Consumer<ServletAsyncListenerWrapper> consumer){
        if(asyncListenerWrapperList != null) {
            for (ServletAsyncListenerWrapper listenerWrapper : asyncListenerWrapperList){
                consumer.accept(listenerWrapper);
            }
        }
    }

    private class ServletAsyncListenerWrapper{
        AsyncListener asyncListener;
        ServletRequest servletRequest;
        ServletResponse servletResponse;

        private ServletAsyncListenerWrapper(AsyncListener asyncListener, ServletRequest servletRequest, ServletResponse servletResponse) {
            this.asyncListener = asyncListener;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
        }
    }

    private class AsyncRuntimeException extends RuntimeException{
        private Throwable cause;
        private AsyncRuntimeException(Throwable cause) {
            this.cause = cause;
        }
        @Override
        public Throwable getCause() {
            return cause;
        }
    }
}
