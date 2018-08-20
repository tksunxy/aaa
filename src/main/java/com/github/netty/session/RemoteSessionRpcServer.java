package com.github.netty.session;

import com.github.netty.core.util.TypeUtil;

import java.net.InetSocketAddress;

/**
 * Created by acer01 on 2018/8/20/020.
 */
public class RemoteSessionRpcServer {

    public RemoteSessionRpcServer(InetSocketAddress address) {

    }

    public static void main(String[] args) {
        int port = TypeUtil.castToInt(System.getProperty("rpc.port"), 8082);
        SessionRpcServer server = new SessionRpcServer(port);

        server.run();
    }

    public void start(){

    }

    public void stop(){

    }

}
