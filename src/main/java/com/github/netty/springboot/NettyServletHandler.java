package com.github.netty.springboot;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.util.ExceptionUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletHandler extends AbstractChannelHandler<FullHttpRequest> {

    private ExecutorService dispatcherExecutor;
    private ServletContext servletContext;

    public NettyServletHandler(ServletContext servletContext) {
        super(false);
        this.servletContext = Objects.requireNonNull(servletContext);
        this.dispatcherExecutor = servletContext.getAsyncExecutorService();
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        ChannelHandlerContext ctxWrap = PartialPooledByteBufAllocator.forceDirectAllocator(ctx);
        HttpServletObject httpServletObject = HttpServletObject.newInstance(servletContext,ctxWrap,fullHttpRequest);

//        Runnable task = newTask2(ctx,httpServletObject);
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
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        };
    }

    private Runnable newTask1(HttpServletObject httpServletObject){
        return new Runnable() {
            @Override
            public void run() {
                ReferenceCountUtil.safeRelease(httpServletObject);
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
                httpServletObject.recycle();
            }
        };
        return task;
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
