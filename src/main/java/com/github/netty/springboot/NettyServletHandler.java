package com.github.netty.springboot;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.core.support.Recyclable;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.util.ExceptionUtil;
import com.github.netty.util.HttpHeaderUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

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
        this.dispatcherExecutor = null;
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext context, FullHttpRequest fullHttpRequest) throws Exception {
        HttpServletObject httpServletObject = HttpServletObject.newInstance(
                servletContext,
                PartialPooledByteBufAllocator.forceDirectAllocator(context),
                fullHttpRequest);

        Runnable task = ServletTask.newInstance(httpServletObject);

//        Runnable task = newTaskForRaw(ctx,fullHttpRequest);

        if(dispatcherExecutor != null) {
            dispatcherExecutor.execute(task);
        }else {
            task.run();
        }
    }

    /**
     * 原生不加业务的代码, 用于测试原生的响应速度
     * @param context
     * @param fullHttpRequest
     * @return
     */
    private Runnable newTaskForRaw(ChannelHandlerContext context, FullHttpRequest fullHttpRequest){
        return () -> {
            boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(fullHttpRequest);
            ByteBuf content = Unpooled.EMPTY_BUFFER;
            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

            HttpHeaderUtil.setKeepAlive(fullHttpResponse, isKeepAlive);
            if (isKeepAlive && !HttpHeaderUtil.isContentLengthSet(fullHttpResponse)) {
                HttpHeaderUtil.setContentLength(fullHttpResponse, content.readableBytes());
            }

            context.writeAndFlush(fullHttpResponse)
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if(!isKeepAlive){
                                future.channel().close();
                            }
                            fullHttpRequest.release();
                        }
                    });
        };
    }

    /**
     * 加spring业务的代码
     * @param context
     * @param fullHttpRequest
     * @return
     */
    private Runnable newTask(ChannelHandlerContext context,FullHttpRequest fullHttpRequest){


        Runnable task = () -> {

        };
        return task;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("发生异常!");
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.channel().close();
        }
    }

    static class ServletTask implements Runnable,Recyclable{
        HttpServletObject httpServletObject;

        private static final AbstractRecycler<ServletTask> RECYCLER = new AbstractRecycler<ServletTask>() {
            @Override
            protected ServletTask newInstance() {
                return new ServletTask();
            }
        };

        private static ServletTask newInstance(HttpServletObject httpServletObject) {
            ServletTask instance = RECYCLER.get();
            instance.httpServletObject = httpServletObject;
            return instance;
        }

        @Override
        public void recycle() {
            httpServletObject = null;
            RECYCLER.recycle(ServletTask.this);
        }

        @Override
        public void run() {
            ServletHttpServletRequest httpServletRequest = httpServletObject.getHttpServletRequest();
            ServletHttpServletResponse httpServletResponse = httpServletObject.getHttpServletResponse();

            try {
                long beginTime = System.currentTimeMillis();
                ServletRequestDispatcher dispatcher = httpServletRequest.getRequestDispatcher(httpServletRequest.getRequestURI());
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
                /*
                 * 每个响应对象是只有当在servlet的service方法的范围内或在filter的doFilter方法范围内是有效的，除非该
                 * 组件关联的请求对象已经开启异步处理。如果相关的请求已经启动异步处理，那么直到AsyncContext的
                 * complete 方法被调用，请求对象一直有效。为了避免响应对象创建的性能开销，容器通常回收响应对象。
                 * 在相关的请求的startAsync 还没有调用时，开发人员必须意识到保持到响应对象引用，超出之上描述的范
                 * 围可能导致不确定的行为
                 */
                if(!httpServletRequest.isAsync()) {
                    httpServletObject.recycle();
                }

                ServletTask.this.recycle();
            }
        }
    }
}
