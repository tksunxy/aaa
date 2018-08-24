package com.github.netty.core.rpc.codec;

/**
 * @author 84215
 */
public interface RpcDataCodec {

    byte[] encodeRequestData(Object[] data);

    Object[] decodeRequestData(byte[] data);

    byte[] encodeResponseData(Object data);

    Object decodeResponseData(byte[] data);

    <T>T cast(Object data,Class<T> type);
}
