package com.github.netty.servlet;

import com.github.netty.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.ReadListener;
import java.io.IOException;


/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletInputStream extends javax.servlet.ServletInputStream {

    private boolean closed; //输入流是否已经关闭，保证线程安全
    private ByteBuf content;
    private ReadListener readListener;

    private int contentLength;

    public ServletInputStream(ByteBuf content) {
        this.closed = false;
        this.content = content;
        this.contentLength = content.capacity();
    }

    public int getContentLength() {
        return contentLength;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        ObjectUtil.checkNotNull(b);
        return super.readLine(b, off, len); //模板方法，会调用当前类实现的read()方法
    }

    /**
     * 本次请求没再有新的HttpContent输入，而且当前的内容全部被读完
     * @return true=读取完毕 反之false
     */
    @Override
    public boolean isFinished() {
        checkNotClosed();
        return content.readableBytes() == 0;
    }

    /**
     * 已读入至少一次HttpContent且未读取完所有内容，或者HttpContent队列非空
     */
    @Override
    public boolean isReady() {
        checkNotClosed();
        return content.readableBytes() > 0;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        checkNotClosed();
        ObjectUtil.checkNotNull(readListener);
        this.readListener = readListener;
    }

    /**
     * 跳过n个字节
     */
    @Override
    public long skip(long n) throws IOException {
        checkNotClosed();
        long skipLen = Math.min(content.readableBytes(), n); //实际可以跳过的字节数
        content.skipBytes((int) skipLen);
        return skipLen;
    }

    /**
     * @return 可读字节数
     */
    @Override
    public int available() throws IOException {
        return null == content ? 0 : content.readableBytes();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        if(content != null && content.refCnt() > 0){
            ReferenceCountUtil.safeRelease(content);
        }
    }

    /**
     * 尝试更新current，然后读取len个字节并复制到b中（off下标开始）
     * @return 实际读取的字节数
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ObjectUtil.checkNotNull(b);
        if (0 == len) {
            return 0;
        }
        if (isFinished()) {
            return -1;
        }
        ByteBuf byteBuf = readContent(len);//读取len个字节
        int readableBytes = byteBuf.readableBytes();
        byteBuf.readBytes(b, off, readableBytes);//复制到b
        return readableBytes - byteBuf.readableBytes();//返回实际读取的字节数
    }

    /**
     * 尝试更新current，然后读取一个字节，并返回
     */
    @Override
    public int read() throws IOException {
        if (isFinished()) {
            return -1;
        }
        return readContent(1).getByte(0);
    }

    /**
     * 从current的HttpContent中读取length个字节
     */
    private ByteBuf readContent(int length) {
        if (length < content.readableBytes()) {
            return content.readSlice(length);
        } else {
            return content;
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Stream is closed");
        }
    }

}
