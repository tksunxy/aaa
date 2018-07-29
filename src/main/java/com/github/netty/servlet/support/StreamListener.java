package com.github.netty.servlet.support;

import io.netty.buffer.ByteBuf;

/**
 * Created by acer01 on 2018/7/28/028.
 */
public interface StreamListener {

    void closeBefore(int totalLength);

    void closeAfter(ByteBuf content);

}
