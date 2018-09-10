package com.github.netty;

import com.github.netty.rpc.RpcServer;
import com.github.netty.session.LocalSessionServiceImpl;

/**
 * 远程会话存储服务 (示例)
 * @author 84215
 */
public class ExampleSessionApplication {

    public static void main(String[] args) {
        Integer port = null;
        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        if(port == null){
            port = 8082;
        }

        run(port);
    }

    public static void run(int port){
        RpcServer server = new RpcServer("Session",port);
        server.addService(new LocalSessionServiceImpl());
        server.run();
    }

//    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
//    private static final String ENCODING_JNU = System.getProperty("sun.jnu.encoding","GBK");
//    private static final String ENCODING_FILE = System.getProperty("file.encoding","UTF-8");
//
//
//    private ResourceLoader resourceLoader;
//    private InetSocketAddress sessionServerAddress;
//    private Properties properties;
//    private InputReadThread inputReadThread;
//    private InputReadThread errorReadThread;
//    private Process process;
//    private Consumer<StartMessage> afterCallback;
//
//    public RemoteSessionApplication(InetSocketAddress sessionServerAddress,ResourceLoader resourceLoader,Consumer<StartMessage> afterCallback) {
//        super(NamespaceUtil.newIdName(RemoteSessionApplication.class));
//        this.resourceLoader = resourceLoader;
//        this.sessionServerAddress = sessionServerAddress;
//        this.properties = System.getProperties();
//        this.afterCallback = afterCallback;
//    }
//
//    @Override
//    public void run() {
//        boolean success = false;
//        String messageStr = "";
//
//        try {
//            if(HostUtil.isWindows()) {
//                process = processByWindowsSystem();
//            }else if(HostUtil.isLinux()){
//                process = processByLinuxSystem();
//            }
//
//            if(process == null){
//                return;
//            }
//
//            inputReadThread = new InputReadThread("In",process.getInputStream());
//            errorReadThread = new InputReadThread("Err",process.getErrorStream());
//            inputReadThread.start();
//            errorReadThread.start();
//
//            success = true;
//        } catch (Exception e) {
//            messageStr = e.getMessage();
//        }finally {
//            StartMessage message = new StartMessage(success,messageStr);
//            afterCallback.accept(message);
//        }
//    }
//
//    private Process processByLinuxSystem(){
//        try {
//            Resource resource = resourceLoader.getResource("start-session.sh");
//            String startCommand = IOUtil.readInput(resource.getInputStream(),ENCODING_FILE);
//
//            URL scriptUrl = resource.getURL();
//            String command = String.format("%1s &",scriptUrl.getPath());
//            return Runtime.getRuntime().exec(command);
//        }catch (Exception e){
//            return null;
//        }
//    }
//
//    private Process processByWindowsSystem(){
//        try {
//            Resource resource = resourceLoader.getResource("start-session.bat");
//            String startCommand = IOUtil.readInput(resource.getInputStream(),ENCODING_FILE);
//
//            URL scriptUrl = resource.getURL();
//            String command = String.format("%1s",scriptUrl.getPath());
//            return Runtime.getRuntime().exec(command);
//        }catch (Exception e){
//            return null;
//        }
//    }
//
//    private class InputReadThread extends Thread{
//        private InputStream inputStream;
//        private BufferedReader bufferedReader;
//
//        public InputReadThread(String preName,InputStream inputStream) throws UnsupportedEncodingException {
//            super(NamespaceUtil.newIdName(preName,InputReadThread.class));
//            this.inputStream = inputStream;
//            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ENCODING_FILE));
//        }
//
//        @Override
//        public void run() {
////            StringBuilder sb = new StringBuilder();
//            try {
//                String line;
//                while ((line = bufferedReader.readLine()) != null) {
////                    sb.append(line).append(LINE_SEPARATOR);
//                    System.out.println("SessionServer : "+line);
//                }
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public class StartMessage {
//        private boolean success;
//        private String message;
//
//        public StartMessage(boolean success, String message) {
//            this.success = success;
//            this.message = message;
//        }
//
//        public String getMessage() {
//            return message;
//        }
//
//        public boolean isSuccess() {
//            return success;
//        }
//    }
}
