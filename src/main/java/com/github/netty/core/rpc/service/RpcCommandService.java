package com.github.netty.core.rpc.service;

import com.github.netty.core.rpc.RpcInterface;

/**
 * rpc命令服务
 * @author acer01
 * 2018/8/20/020
 */
@RpcInterface(value = "/_inner/command",timeout = 1000)
public interface RpcCommandService {

    /**
     * ping
     * @return
     */
    byte[] ping();

}
