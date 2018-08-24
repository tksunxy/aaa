package com.github.netty.core.rpc.exception;

/**
 * rpc调用响应异常
 * Created by acer01 on 2018/8/21/021.
 */
public class RpcResponseException extends RpcException {

    /**
     * 错误状态码
     */
    private int status;

    public RpcResponseException(int status,String message) {
        super(message, null, false, false);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
