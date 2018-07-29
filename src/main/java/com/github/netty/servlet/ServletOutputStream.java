package com.github.netty.servlet;

import com.github.netty.servlet.support.StreamListener;
import com.github.netty.util.ObjectUtil;
import com.github.netty.util.TodoOptimize;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;


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
    private StreamListener streamListener;
    private HttpResponse nettyResponse;
    @TodoOptimize("缺少对keep-alive的支持")
    private boolean isKeepAlive;

    ServletOutputStream(ChannelHandlerContext ctx, HttpResponse nettyResponse,boolean isKeepAlive) {
        this.isKeepAlive = isKeepAlive;
        this.ctx = ctx;
        this.nettyResponse = nettyResponse;
        this.compositeByteBuf = new CompositeByteBuf(ctx.alloc(),false,16);
        this.closed = false;
    }

    public void setStreamListener(StreamListener streamListener) {
        this.streamListener = streamListener;
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

    @Override
    public void close() throws IOException {
        synchronized (syncLock) {
            if (closed) {
                return;
            }

            try {
                int cap = compositeByteBuf.capacity();
                compositeByteBuf.writerIndex(cap);

                streamListener.closeBefore(cap);

                ctx.write(nettyResponse, ctx.voidPromise());
                ctx.write(new DefaultLastHttpContent(compositeByteBuf))
                        .addListener(new ChannelFutureListener() {
                            /**
                             * 写完后1.刷新 2.释放内存 3.关闭流
                             * @param future 回调对象
                             * @throws Exception 异常
                             */
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                ctx.flush();
                                if(compositeByteBuf.refCnt() > 0) {
                                    ReferenceCountUtil.safeRelease(compositeByteBuf);
                                }
                                future.channel().close();
                            }
                        });
            }catch (Throwable e){
                errorEvent(e);
            }
            closed = true;
        }
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
