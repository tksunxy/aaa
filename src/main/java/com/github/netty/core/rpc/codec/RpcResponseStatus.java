package com.github.netty.core.rpc.codec;

/**
 *  rpc响应状态
 * Created by acer01 on 2018/8/25/025.
 */
public class RpcResponseStatus {

    //正常返回
    public static final int OK = 200;
    //找不到方法
    public static final int NO_SUCH_METHOD = 400;
    //找不到服务
    public static final int NO_SUCH_SERVICE = 401;
    //服务器错误
    public static final int SERVER_ERROR = 500;


}
