package com.github.netty.rpc.exception;

/**
 * 超时异常
 * Created by acer01 on 2018/8/20/020.
 */
public class RpcTimeoutException extends RpcException {

    public RpcTimeoutException(String message) {
        super(message, null, false, true);
    }

}
