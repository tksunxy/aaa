package com.github.netty.servlet;

import com.github.netty.servlet.support.StreamListener;
import com.github.netty.util.ExceptionUtil;
import com.github.netty.util.ObjectUtil;
import com.github.netty.util.TodoOptimize;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;


/**
 * 需要对keep-alive的支持
 * @author 84215
 */
public class ServletOutputStream extends javax.servlet.ServletOutputStream {

    private final Object syncLock = new Object();
    //是否已经调用close()方法关闭输出流
    private boolean closed;
    //监听器，暂时没处理
    private WriteListener writeListener;
    private ChannelHandlerContext ctx;
    private CompositeByteBuf compositeByteBuf;
    private List<StreamListener> streamListenerList;
    private HttpResponse nettyResponse;
    @TodoOptimize("缺少对keep-alive的支持")
    private boolean isKeepAlive;

    ServletOutputStream(ChannelHandlerContext ctx, HttpResponse nettyResponse,boolean isKeepAlive) {
        this.isKeepAlive = isKeepAlive;
        this.ctx = ctx;
        this.nettyResponse = nettyResponse;
        this.compositeByteBuf = new CompositeByteBuf(ctx.alloc(),false,16);
        this.streamListenerList = new LinkedList<>();
        this.closed = false;
    }

    public void addStreamListener(StreamListener streamListener) {
        this.streamListenerList.add(streamListener);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        ObjectUtil.checkNotNull(writeListener);
        //只能设置一次
        if(this.writeListener != null){
            return;
        }
        this.writeListener = writeListener;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();

        if(len <= 0){
            return;
        }
        ByteBuf content = ctx.alloc().buffer(len);
        content.writeBytes(b, off, len);
        compositeByteBuf.addComponent(content);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkClosed();

        write(b,0,b.length);
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();

        ByteBuf content = ctx.alloc().buffer(4);
        content.writeInt(b);
        compositeByteBuf.addComponent(content);
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
    }

    public int getContentLength(){
        return compositeByteBuf.capacity();
    }

    /**
     * 结束响应对象
     * 当响应被关闭时，容器必须立即刷出响应缓冲区中的所有剩余的内容到客户端。
     * 以下事件表明servlet满足了请求且响应对象即将关闭：
     * ■servlet的service方法终止。
     * ■响应的setContentLength或setContentLengthLong方法指定了大于零的内容量，且已经写入到响应。
     * ■sendError 方法已调用。
     * ■sendRedirect 方法已调用。
     * ■AsyncContext 的complete 方法已调用
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        synchronized (syncLock) {
            if (closed) {
                return;
            }

            try {
                int cap = compositeByteBuf.capacity();
                compositeByteBuf.writerIndex(cap);

                for(StreamListener streamListener : streamListenerList) {
                    streamListener.closeBefore(cap);
                }

                ctx.write(nettyResponse, ctx.voidPromise());

                ctx.writeAndFlush(buildContent(compositeByteBuf))
                        .addListener(new ChannelFutureListener() {
                            /**
                             * 写完后1.刷新 2.释放内存 3.关闭流
                             * @param future 回调对象
                             * @throws Exception 异常
                             */
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                try {
                                    for (StreamListener streamListener : streamListenerList) {
                                        streamListener.closeAfter(compositeByteBuf);
                                    }
//                                    if(!isKeepAlive){
                                        future.channel().close();
//                                    }
                                }catch (Throwable throwable){
                                    ExceptionUtil.printRootCauseStackTrace(throwable);
                                }
                            }
                        });
            }catch (Throwable e){
                ExceptionUtil.printRootCauseStackTrace(e);
                errorEvent(e);
            }
            closed = true;
        }
    }

    private HttpContent buildContent(ByteBuf byteBuf){
        HttpContent httpContent;
        if(isKeepAlive){
            httpContent = new DefaultLastHttpContent(byteBuf);
        }else {
            httpContent = new DefaultLastHttpContent(byteBuf);
        }
        return httpContent;
    }

    private void errorEvent(Throwable throwable){
        if(writeListener != null){
            writeListener.onError(throwable);
        }
    }

    private void checkClosed() throws ClosedChannelException {
        if(isClosed()){
            throw new ClosedChannelException();
        }
    }

    public boolean isClosed() {
        return closed;
    }

}
