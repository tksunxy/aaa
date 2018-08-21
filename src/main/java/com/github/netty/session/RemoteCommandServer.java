package com.github.netty.session;

import com.github.netty.core.util.TypeUtil;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Created by acer01 on 2018/8/20/020.
 */
public class RemoteCommandServer {

    public RemoteCommandServer(InetSocketAddress address) {

    }

    public static void main(String[] args) {
        int port = TypeUtil.castToInt(System.getProperty("rpc.port"), 8082);
        SessionRpcServer server = new SessionRpcServer(port);

        server.run();
    }

    public RemoteCommandServer executeCommand(String command,Consumer<CommandResult> callback){
        CommandResult result = new CommandResult(true);
        callback.accept(result);
        return this;
    }

    public void stop(){

    }

    public class CommandResult {
        private boolean success;

        public CommandResult(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

    }

}
