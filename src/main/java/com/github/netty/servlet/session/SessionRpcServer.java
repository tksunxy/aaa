package com.github.netty.servlet.session;

import com.github.netty.core.rpc.RpcServer;
import com.github.netty.servlet.session.impl.LocalSessionServiceImpl;

import java.net.InetSocketAddress;

/**
 *
 * @author acer01
 * 2018/8/18/018
 */
public class SessionRpcServer extends RpcServer{

    public SessionRpcServer(int port) {
        this(new InetSocketAddress(port));
    }

    public SessionRpcServer(InetSocketAddress address) {
        super("",address);
        addService(new LocalSessionServiceImpl());
    }


}
