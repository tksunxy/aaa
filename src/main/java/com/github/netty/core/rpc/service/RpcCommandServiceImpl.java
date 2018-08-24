package com.github.netty.core.rpc.service;

/**
 *
 * @author acer01
 * 2018/8/20/020
 */
public class RpcCommandServiceImpl implements RpcCommandService {

    @Override
    public byte[] ping() {
        return "ok".getBytes();
    }

}
