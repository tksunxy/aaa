package com.github.netty.core.rpc;

/**
 * Created by acer01 on 2018/8/20/020.
 */
public class RpcCommandServiceImpl implements RpcCommandService {

    @Override
    public byte[] ping() {
        return "ok".getBytes();
    }

}
