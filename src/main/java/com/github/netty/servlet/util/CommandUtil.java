package com.github.netty.servlet.util;

import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.core.util.TodoOptimize;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * 远程命令执行
 * @author acer01
 *  2018/8/20/020
 */
public class CommandUtil {

    @TodoOptimize("待实现, 登录远程ssh,执行命令启动另一个jvm")
    public static void execSshCommand(String command, Consumer<CommandResult> callback){

    }

    @TodoOptimize("待实现, 执行本地脚本命令启动另一个jvm")
    public static void execLocalCommand(String command, Consumer<CommandResult> callback){
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

                CommandResult result = new CommandResult(true,sb.toString());
                callback.accept(result);
            } catch (Exception e) {
                CommandResult result = new CommandResult(false,sb.toString());
                callback.accept(result);
            }
        });
    }


    public static class CommandResult {
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
