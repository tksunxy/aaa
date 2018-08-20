package com.github.netty.session;

import com.github.netty.core.rpc.RpcServer;

import java.net.InetSocketAddress;

/**
 * Created by acer01 on 2018/8/18/018.
 */
public class SessionRpcServer extends RpcServer{

    public SessionRpcServer(int port) {
        this(new InetSocketAddress(port));
    }

    public SessionRpcServer(InetSocketAddress address) {
        super("",address);
        addService(new SessionServiceImpl());
    }


}
