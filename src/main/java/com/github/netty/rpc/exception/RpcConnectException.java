package com.github.netty.rpc.exception;

/**
 * 连接异常
 * Created by acer01 on 2018/8/21/021.
 */
public class RpcConnectException extends RpcException {

    public static final RpcConnectException INSTANCE = new RpcConnectException("The channel no connect");

    public RpcConnectException(String message) {
        super(message, null, false, false);
    }

}
