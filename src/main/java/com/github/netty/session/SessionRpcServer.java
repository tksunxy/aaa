package com.github.netty.session;

import com.github.netty.core.rpc.RpcServer;
import com.github.netty.core.util.TodoOptimize;
import com.github.netty.core.util.TypeUtil;

import java.net.InetSocketAddress;

/**
 * Created by acer01 on 2018/8/18/018.
 */
@TodoOptimize("保存session接口接不到数据,调用丢失")
public class SessionRpcServer extends RpcServer{

    public SessionRpcServer(int port) {
        super(port);
    }

    public SessionRpcServer(InetSocketAddress address) {
        super(address);
    }

    public static void main(String[] args) {
        int port = TypeUtil.castToInt(System.getProperty("rpc.port"), 8082);
        SessionRpcServer server = new SessionRpcServer(port);

        server.addService(SessionService.SERVICE_NAME,new SessionServiceImpl());
        server.run();
    }

    public void start(){

    }

    public void stop(){
        super.stop();
    }


}
