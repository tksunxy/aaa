package com.github.netty.springboot;

import com.github.netty.core.adapter.AbstractChannelHandler;
import com.github.netty.servlet.ServletAsyncContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.util.ExceptionUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletDispatcherHandler extends AbstractChannelHandler<HttpServletObject> {

    private ExecutorService dispatcherExecutor;

    public NettyServletDispatcherHandler(ExecutorService dispatcherExecutor) {
        this.dispatcherExecutor = dispatcherExecutor;
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, HttpServletObject httpServletObject) throws Exception {
        Runnable task = newTask(httpServletObject);
        if(dispatcherExecutor != null) {
            dispatcherExecutor.execute(task);
        }else {
            task.run();
        }
    }

    private Runnable newTask2(ChannelHandlerContext ctx,HttpServletObject httpServletObject){
        return new Runnable() {
            @Override
            public void run() {
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        };
    }

    private Runnable newTask1(HttpServletObject httpServletObject){
        return new Runnable() {
            @Override
            public void run() {
                ServletHttpServletRequest httpServletRequest = httpServletObject.getHttpServletRequest();
                ServletHttpServletResponse httpServletResponse = httpServletObject.getHttpServletResponse();
                try {
                    httpServletResponse.getOutputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Runnable newTask(HttpServletObject httpServletObject){
        Runnable task = () -> {
            ServletHttpServletRequest httpServletRequest = httpServletObject.getHttpServletRequest();
            ServletHttpServletResponse httpServletResponse = httpServletObject.getHttpServletResponse();

            try {
                long beginTime = System.currentTimeMillis();
                ServletRequestDispatcher dispatcher = httpServletRequest.getServletContext().getRequestDispatcher(httpServletRequest.getRequestURI());
                if (dispatcher == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                dispatcher.dispatch(httpServletRequest, httpServletResponse, DispatcherType.REQUEST);
                long totalTime = System.currentTimeMillis() - beginTime;
                if(totalTime > 10) {
                    System.out.println("-" + (totalTime)+" 请求耗时");
                }
            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }finally {
                closeServlet(httpServletRequest,httpServletResponse);
            }
        };
        return task;
    }

    /**
     * 关闭servlet
     * @param httpServletRequest
     * @param httpServletResponse
     */
    private void closeServlet(ServletHttpServletRequest httpServletRequest,ServletHttpServletResponse httpServletResponse){
        try {
            httpServletRequest.getInputStream().close();
        }catch (Throwable throwable){
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
            if(asyncContext == null || !asyncContext.isStarted()) {
                httpServletResponse.getOutputStream().close();
            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.channel().close();
        }
    }


}
