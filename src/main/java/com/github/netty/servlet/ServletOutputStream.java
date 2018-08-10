package com.github.netty.servlet;

import com.github.netty.core.support.Wrapper;
import com.github.netty.servlet.support.ChannelInvoker;
import com.github.netty.util.ExceptionUtil;
import com.github.netty.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;


/**
 * 需要对keep-alive的支持
 * @author 84215
 */
public class ServletOutputStream extends javax.servlet.ServletOutputStream implements Wrapper<CompositeByteBuf>{

    private final Object syncLock = new Object();
    //是否已经调用close()方法关闭输出流
    private boolean closed;
    //监听器，暂时没处理
    private WriteListener writeListener;
    private CompositeByteBuf source;
    private ChannelInvoker channelInvoker;

    ServletOutputStream() {
    }

    public void setChannelInvoker(ChannelInvoker channelInvoker) {
        this.channelInvoker = channelInvoker;
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
        ByteBuf content = this.source.alloc().buffer(len);
        content.writeBytes(b, off, len);
        this.source.addComponent(content);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkClosed();

        write(b,0,b.length);
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();

        ByteBuf content = this.source.alloc().buffer(4);
        content.writeInt(b);
        this.source.addComponent(content);
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
    }

    public int getContentLength(){
        return source.capacity();
    }


    @Override
    public void close() throws IOException {
        close(null);
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
    public void close(ChannelFutureListener finishListener) throws IOException {
        synchronized (syncLock) {
            if (closed) {
                return;
            }

            try {
                source.writerIndex(source.capacity());

                ChannelFutureListener releaseListener = newReleaseListener();
                ChannelFutureListener[] finishListeners = finishListener == null?
                        new ChannelFutureListener[]{releaseListener} : new ChannelFutureListener[]{finishListener, releaseListener};

                channelInvoker.writeAndFlushAndIfNeedClose(source,finishListeners);
            }catch (Throwable e){
                ExceptionUtil.printRootCauseStackTrace(e);
                errorEvent(e);
            }
            closed = true;
        }
    }

    private ChannelFutureListener newReleaseListener(){
        ChannelFutureListener releaseListener = future -> {
            if(source.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(source);
            }
        };
        return releaseListener;
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

    @Override
    public void wrap(CompositeByteBuf source) {
        this.source = source;
        this.closed = false;
    }

    @Override
    public CompositeByteBuf unwrap() {
        return source;
    }

}
