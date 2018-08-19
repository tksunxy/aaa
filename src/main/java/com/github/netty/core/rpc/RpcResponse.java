package com.github.netty.core.rpc;

import java.io.Serializable;

/**
 * Created by acer01 on 2018/8/19/019.
 */
public class RpcResponse implements Serializable{

    //正常返回
    public static final int OK = 200;
    //找不到方法
    public static final int NO_SUCH_METHOD = 400;
    //找不到服务
    public static final int NO_SUCH_SERVICE = 401;
    //服务器错误
    public static final int SERVER_ERROR = 500;

    //请求ID
    private Integer requestId;
    //响应状态
    private int status;
    //响应信息
    private String message;
    //响应数据
    private Object data;

    public RpcResponse() {}

    public RpcResponse(int status,String message) {
        this.status = status;
        this.message = message;
    }


    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId=" + requestId +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
