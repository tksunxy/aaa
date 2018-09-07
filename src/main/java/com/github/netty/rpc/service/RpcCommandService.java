package com.github.netty.rpc.service;

import com.github.netty.rpc.RpcInterface;

/**
 * rpc命令服务
 * @author acer01
 * 2018/8/20/020
 */
@RpcInterface(value = "/_inner/command",timeout = 1000 * 5)
public interface RpcCommandService {

    /**
     * ping
     * @return
     */
    byte[] ping();

}
