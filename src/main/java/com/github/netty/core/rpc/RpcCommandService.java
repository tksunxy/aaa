package com.github.netty.core.rpc;

/**
 * Created by acer01 on 2018/8/20/020.
 */
@RpcInterface(value = "/_inner/CommandService",timeout = 1000)
public interface RpcCommandService {

    byte[] ping();

}
