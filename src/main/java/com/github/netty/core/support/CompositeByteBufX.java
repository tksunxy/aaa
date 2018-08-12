package com.github.netty.core.support;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by acer01 on 2018/8/11/011.
 */
public class CompositeByteBufX extends io.netty.buffer.CompositeByteBuf {

    public CompositeByteBufX(boolean direct, int maxNumComponents) {
        super(PartialPooledByteBufAllocator.INSTANCE, direct, maxNumComponents);
    }

    public CompositeByteBufX(ByteBufAllocator alloc, boolean direct, int maxNumComponents) {
        super(alloc, direct, maxNumComponents);
    }

    public CompositeByteBufX(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteBuf... buffers) {
        super(alloc, direct, maxNumComponents, buffers);
    }

    public CompositeByteBufX(ByteBufAllocator alloc, boolean direct, int maxNumComponents, Iterable<ByteBuf> buffers) {
        super(alloc, direct, maxNumComponents, buffers);
    }

}
