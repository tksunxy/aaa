package com.github.netty;

import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;
import com.github.netty.core.util.HostUtil;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.core.util.TypeUtil;
import com.github.netty.session.SessionRpcServer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * 远程会话存储服务
 * @author 84215
 */
public class RemoteSessionApplication extends Thread{

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String ENCODING_JNU = System.getProperty("sun.jnu.encoding","GBK");
    private static final String ENCODING_FILE = System.getProperty("file.encoding","UTF-8");

    private static final String SCRIPT_STYLE_WINDOWS = "%1s";
    private static final String SCRIPT_STYLE_LINUX = "%1s &";

    private ResourceLoader resourceLoader;
    private InetSocketAddress sessionServerAddress;
    private Properties properties;
    private InputReadThread inputReadThread;
    private InputReadThread errorReadThread;
    private Process process;
    private Consumer<StartMessage> afterCallback;

    public RemoteSessionApplication(InetSocketAddress sessionServerAddress,ResourceLoader resourceLoader,Consumer<StartMessage> afterCallback) {
        super(NamespaceUtil.newIdName(RemoteSessionApplication.class));
        this.resourceLoader = resourceLoader;
        this.sessionServerAddress = sessionServerAddress;
        this.properties = System.getProperties();
        this.afterCallback = afterCallback;
    }

    public static void main(String[] args) {
        int port = TypeUtil.castToInt(System.getProperty("rpc.port"), 8082);
        SessionRpcServer server = new SessionRpcServer(port);
        server.run();
    }

    @Override
    public void run() {
        Resource resource = resourceLoader.getResource("start-session.bat");
        try {
            URL scriptUrl = resource.getURL();
            String scriptPath = scriptUrl.getPath();
            String scriptStyle = HostUtil.isWindows()? SCRIPT_STYLE_WINDOWS : SCRIPT_STYLE_LINUX;
            String command =  String.format(scriptStyle,scriptPath);

            process = Runtime.getRuntime().exec(command);
            inputReadThread = new InputReadThread("In",process.getInputStream());
            errorReadThread = new InputReadThread("Err",process.getErrorStream());

            inputReadThread.start();
            errorReadThread.start();

            StartMessage message = new StartMessage(true,"");
            afterCallback.accept(message);
        } catch (Exception e) {
            StartMessage message = new StartMessage(false,"");
            afterCallback.accept(message);
        }
    }

    private class InputReadThread extends Thread{
        private InputStream inputStream;
        private BufferedReader bufferedReader;

        public InputReadThread(String preName,InputStream inputStream) throws UnsupportedEncodingException {
            super(NamespaceUtil.newIdName(preName,InputReadThread.class));
            this.inputStream = inputStream;
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ENCODING_FILE));
        }

        @Override
        public void run() {
//            StringBuilder sb = new StringBuilder();
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
//                    sb.append(line).append(LINE_SEPARATOR);
                    System.out.println("SessionServer : "+line);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public class StartMessage {
        private boolean success;
        private String message;

        public StartMessage(boolean success, String message) {
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
