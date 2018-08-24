package com.github.netty.core.rpc.codec;

/**
 *  数据编码解码器
 * @author 84215
 */
public interface DataCodec {

    /**
     * 请求数据编码
     * @param data
     * @return
     */
    byte[] encodeRequestData(Object[] data);

    /**
     * 请求数据解码
     * @param data
     * @return
     */
    Object[] decodeRequestData(byte[] data);

    /**
     * 响应数据编码
     * @param data
     * @return
     */
    byte[] encodeResponseData(Object data);

    /**
     * 响应数据解码
     * @param data
     * @return
     */
    Object decodeResponseData(byte[] data);

    /**
     * 类型转换
     * @param data
     * @param type
     * @param <T>
     * @return
     */
    <T>T cast(Object data,Class<T> type);

}
