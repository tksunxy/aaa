package com.github.netty.session;

import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.core.util.TodoOptimize;
import com.github.netty.core.util.TypeUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    @TodoOptimize("待实现, 登录远程ssh,执行命令启动另一个jvm")
    public RemoteCommandServer execSshCommand(String command, Consumer<CommandResult> callback){
        return this;
    }

    @TodoOptimize("待实现, 执行本地脚本命令启动另一个jvm")
    public RemoteCommandServer execLocalCommand(String command, Consumer<CommandResult> callback){
        ThreadPoolX.getDefaultInstance().execute(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                String lineSeparator = System.getProperty("line.separator");
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader bufferedReader = new BufferedReader
                        (new InputStreamReader(process.getInputStream(),System.getProperty("sun.jnu.encoding","GBK")));
                String line;
                while ((line = bufferedReader.readLine()) != null && line.length() > 0) {
                    sb.append(line).append(lineSeparator);
                }
                process.exitValue();
            } catch (Exception e) {
                //
            }

            CommandResult result = new CommandResult(true,sb.toString());
            callback.accept(result);
        });
        return this;
    }

    public void stop(){

    }

    public class CommandResult {
        private boolean success;
        private String message;

        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return success;
        }

    }
}
