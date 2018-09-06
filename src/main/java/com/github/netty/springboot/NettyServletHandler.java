package com.github.netty.springboot;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.Optimize;
import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.HttpHeaderUtil;
import com.github.netty.servlet.*;
import com.github.netty.servlet.support.HttpServletObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletHandler extends AbstractChannelHandler<FullHttpRequest> {

    private Executor dispatcherExecutor;
    private ServletContext servletContext;

    public static AtomicLong SERVLET_AND_FILTER_TIME = new AtomicLong();
    public static AtomicLong SERVLET_QUERY_COUNT = new AtomicLong();

    public NettyServletHandler(ServletContext servletContext) {
        super(false);
        this.servletContext = Objects.requireNonNull(servletContext);
        this.dispatcherExecutor = Optimize.getServletHandlerExecutor();
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext context, FullHttpRequest fullHttpRequest) throws Exception {
        Runnable task;
        if(Optimize.isEnableRawNetty()) {
            task = newTaskForRaw(context,fullHttpRequest);
        }else {
            HttpServletObject httpServletObject = HttpServletObject.newInstance(
                    servletContext,
                    PartialPooledByteBufAllocator.forceDirectAllocator(context),
                    fullHttpRequest);
            task = ServletTask.newInstance(httpServletObject);
        }

        if(dispatcherExecutor != null) {
            dispatcherExecutor.execute(task);
        }else {
            task.run();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        saveAndClearSession(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("ServletHandler 异常! : "+cause.toString());
        saveAndClearSession(ctx);
        ctx.channel().close();
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
            ByteBuf content = Unpooled.wrappedBuffer("ok".getBytes());
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
     * 保存并且清空会话
     * @param ctx
     */
    private void saveAndClearSession(ChannelHandlerContext ctx){
        Attribute<ServletHttpSession> attribute = ctx.channel().attr(HttpServletObject.CHANNEL_ATTR_KEY_SESSION);
        ServletHttpSession httpSession = attribute.getAndSet(null);
        if(httpSession == null) {
            return;
        }

        if (httpSession.isValid()) {
            servletContext.getSessionService().saveSession(httpSession.unwrap());
            logger.info("同步会话 : id="+httpSession.getId());
        } else if (httpSession.getId() != null) {
            servletContext.getSessionService().removeSession(httpSession.getId());
            logger.info("移除会话 : id="+httpSession.getId());
        }
        httpSession.clear();
    }

    /**
     * servlet任务
     */
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

            long beginTime = System.currentTimeMillis();
            try {
                ServletRequestDispatcher dispatcher = httpServletRequest.getRequestDispatcher(httpServletRequest.getRequestURI());
                if (dispatcher == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                dispatcher.dispatch(httpServletRequest, httpServletResponse, DispatcherType.REQUEST);

            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }finally {
                long totalTime = System.currentTimeMillis() - beginTime;
                SERVLET_AND_FILTER_TIME.addAndGet(totalTime);
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


                SERVLET_QUERY_COUNT.incrementAndGet();
            }
        }
    }
}
