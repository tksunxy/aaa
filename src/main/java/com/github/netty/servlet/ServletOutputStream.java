package com.github.netty.servlet;

import com.github.netty.core.support.CompositeByteBufX;
import com.github.netty.core.support.Wrapper;
import com.github.netty.core.util.IOUtil;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.servlet.support.ChannelInvoker;
import com.github.netty.servlet.support.HttpServletObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;

import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 需要对keep-alive的支持
 * @author 84215
 */
public class ServletOutputStream extends javax.servlet.ServletOutputStream implements Wrapper<CompositeByteBufX>{

    //是否已经调用close()方法关闭输出流
    private AtomicBoolean closed = new AtomicBoolean(false);
    private ChannelInvoker channelInvoker = new ChannelInvoker();
    //监听器，暂时没处理
    private WriteListener writeListener;
    private CompositeByteBufX source;
    private HttpServletObject httpServletObject;

    ServletOutputStream() {
    }

    public void setHttpServletObject(HttpServletObject httpServletObject) {
        this.httpServletObject = httpServletObject;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        Objects.requireNonNull(writeListener);
        //只能设置一次
        if(this.writeListener != null){
            return;
        }
        this.writeListener = writeListener;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();

        if(len == 0){
            return;
        }

        ByteBuf content = Unpooled.wrappedBuffer(b,off,len);
        this.source.addComponent(content);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b,0,b.length);
    }

    @Override
    public void write(int b) throws IOException {
        int byteLen = 4;
        byte[] bytes = new byte[byteLen];
        IOUtil.setInt(bytes,0,b);
        write(bytes,0,byteLen);
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
        if (closed.compareAndSet(false,true)) {
            try {
                source.writerIndex(source.capacity());

//                ChannelFutureListener releaseListener = newReleaseListener();
//                ChannelFutureListener[] finishListeners = finishListener == null ?
//                        new ChannelFutureListener[]{releaseListener} : new ChannelFutureListener[]{releaseListener,finishListener};

                ChannelFutureListener[] finishListeners = finishListener == null? null : new ChannelFutureListener[]{finishListener};

                channelInvoker.writeAndReleaseFlushAndIfNeedClose(httpServletObject, source, finishListeners);
            }catch(Throwable e){
                ExceptionUtil.printRootCauseStackTrace(e);
                errorEvent(e);
            }
        }else {
            try {
                finishListener.operationComplete(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ChannelFutureListener newReleaseListener(){
        ChannelFutureListener releaseListener = future -> {
            try {
                if(source.refCnt() > 0){
                    source.release();
                }
                source = null;
            }catch (Exception e){
                e.printStackTrace();
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
        if(closed.get()){
            throw new ClosedChannelException();
        }
    }

    @Override
    public void wrap(CompositeByteBufX source) {
        this.source = source;
        this.closed.set(false);
    }

    @Override
    public CompositeByteBufX unwrap() {
        return source;
    }

}
