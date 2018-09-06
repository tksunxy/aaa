package com.github.netty;

import com.github.netty.core.util.TypeUtil;
import com.github.netty.servlet.session.SessionRpcServer;

/**
 * 远程会话存储服务
 * @author 84215
 */
public class RemoteSessionApplication {

    public static void main(String[] args) {
        int port = TypeUtil.castToInt(System.getProperty("rpc.port"), 8082);
        SessionRpcServer server = new SessionRpcServer(port);
        server.run();
    }

}
