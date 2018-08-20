package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyClient;
import com.github.netty.core.rpc.exception.RpcConnectException;
import com.github.netty.core.rpc.exception.RpcTimeoutException;
import com.github.netty.core.util.ReflectUtil;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by acer01 on 2018/8/18/018.
 */
public class RpcClient extends AbstractNettyClient implements InvocationHandler {

    private RpcCommandService commandService;
    private Timer timer;

    //不需要代理的方法
    private static final Collection<String> NO_PROXY_METHOD_LIST;
    static {
        NO_PROXY_METHOD_LIST = new HashSet<>();
        for(Method method : Object.class.getMethods()){
            NO_PROXY_METHOD_LIST.add(method.getName());
        }
    }

    //请求锁
    private final Map<Integer,RpcLock> requestLockMap = new HashMap<>();
    //生成请求id
    private final AtomicInteger requestIdIncr = new AtomicInteger();
    //实例配置项
    private final Map<Class,RpcInstanceConfig> instanceConfigMap = new HashMap<>();


    public RpcClient(String remoteHost, int remotePort) {
        this(new InetSocketAddress(remoteHost, remotePort));
    }

    public RpcClient(InetSocketAddress address) {
        this("",address);
    }

    public RpcClient(String namePre, InetSocketAddress remoteAddress) {
        super(namePre, remoteAddress);
        run();
    }

    /**
     * 开启自动重连
     * @param heartIntervalSecond 心跳检测间隔
     */
    public void enableAutoReconnect(int heartIntervalSecond){
        timer = new Timer();
        RpcHeartbeatTask heartbeatTask = new RpcHeartbeatTask();
        timer.schedule(heartbeatTask,0,heartIntervalSecond);
    }

    /**
     * 新建请求id
     * @return
     */
    protected Integer newRequestId(){
        return requestIdIncr.incrementAndGet();
    }

    /**
     * 新建实例
     * @param clazz 接口 interface
     * @param <T>
     * @return  实例
     */
    public <T>T newInstance(Class<T> clazz){
       RpcInterface rpcInterfaceAnn = ReflectUtil.findAnnotation(clazz, RpcInterface.class);
        if (rpcInterfaceAnn == null) {
            //缺少RpcInterface注解
            throw new IllegalStateException("The interface is not exist Annotation, you must coding @RpcInterface");
        }

        synchronized (instanceConfigMap) {
            RpcInstanceConfig config = instanceConfigMap.get(clazz);
            if (config != null) {
                //同一个接口不允许在同一个客户端上建立多个实例
                throw new IllegalStateException("The same interface does not allow multiple instances of the same client");
            }

            String serviceName = rpcInterfaceAnn.value();
            int timeout = rpcInterfaceAnn.timeout();
            instanceConfigMap.put(clazz, new RpcInstanceConfig(timeout, serviceName));
        }

        T instance = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return instance;
    }

    /**
     * 初始化所有处理器
     * @return
     */
    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<Channel>() {
            private ChannelHandler handler = new RpcClientHandler();
            private ObjectEncoder encoder = new ObjectEncoder();

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast(encoder);
                pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                pipeline.addLast(handler);
            }
        };
    }

    /**
     * 获取链接
     * @return
     */
    @Override
    public SocketChannel getSocketChannel() {
        SocketChannel socketChannel = super.getSocketChannel();
        if(socketChannel == null){
            throw RpcConnectException.INSTANCE;
        }
        return socketChannel;
    }

    /**
     * 进行rpc调用
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if(NO_PROXY_METHOD_LIST.contains(methodName)){
            return method.invoke(this,args);
        }

        Integer requestId = newRequestId();
        RpcInstanceConfig instanceConfig = instanceConfigMap.get(method.getDeclaringClass());

        RpcRequest rpcRequest = new RpcRequest(requestId);
        rpcRequest.setServiceName(instanceConfig.serviceName);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParametersVal(args);

        getSocketChannel().writeAndFlush(rpcRequest);

        RpcLock lock = new RpcLock();
        requestLockMap.put(requestId,lock);
        //上锁, 等待服务端响应释放锁
        RpcResponse rpcResponse = lock.lock(instanceConfig.timeout);
        //移除锁
        requestLockMap.remove(requestId);

        if(rpcResponse != null && rpcResponse.getStatus() == RpcResponse.OK){
            return rpcResponse.getData();
        }
        return null;
    }

    @Override
    protected void startAfter() {
        super.startAfter();
        commandService = newInstance(RpcCommandService.class);

        //开启自动重连, 默认10秒心跳检测一次
        enableAutoReconnect(10 * 1000);
    }

    /**
     * 客户端处理器
     */
    @ChannelHandler.Sharable
    private class RpcClientHandler extends AbstractChannelHandler<RpcResponse> {
        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
            RpcLock lock = requestLockMap.get(rpcResponse.getRequestId());
            //如果获取不到锁 说明已经超时, 被释放了
            if(lock == null){
                return;
            }
            lock.unlock(rpcResponse);
        }
    }

    /**
     * 心跳任务
     */
    private class RpcHeartbeatTask extends TimerTask{
        /**
         * 重连次数
         */
        private int reconnectCount;
        /**
         * 最大超时重试次数
         */
        private int maxTimeoutRetryNum = 6;

        private boolean reconnect(String causeMessage){
            int count = ++reconnectCount;
            boolean success = connect();

            String logMsg;
            if (success) {
                reconnectCount = 0;
                logMsg = "第[" + count + "]次断线重连 : 成功" + getSocketChannel()+", 重连原因["+ causeMessage +"]";
            } else {
                logMsg = "第[" + count + "]次断线重连 : 失败, 重连原因["+ causeMessage +"]";;
            }
            logger.info(logMsg);
            return success;
        }

        @Override
        public void run() {
            try {
                byte[] msg = commandService.ping();
                logger.info("心跳包 : " + new String(msg));

            }catch (RpcConnectException e) {
                reconnect(e.getMessage());

            }catch (RpcTimeoutException e){
                //重ping N次, 如果N次后还ping不通, 则进行重连
                for(int i = 0; i< maxTimeoutRetryNum; i++) {
                    try {
                        byte[] msg = commandService.ping();
                        return;
                    } catch (RpcConnectException e1) {
                        reconnect(e1.getMessage());
                        return;
                    }catch (RpcTimeoutException e2){
                        //
                    }
                }
                reconnect(e.getMessage());
            }
        }
    }

    /**
     * 远程调用后,等待响应的同步锁
     */
    private class RpcLock{
        private long beginTime;
        private RpcResponse rpcResponse;

        private RpcResponse lock(int timeout) throws InterruptedException, TimeoutException {
            this.beginTime = System.currentTimeMillis();
            synchronized (RpcLock.this){
                wait(timeout);
            }

            if(rpcResponse == null){
                throw new RpcTimeoutException("requestTimeout : maxTimeout is ["+timeout+"]");
            }
            return rpcResponse;
        }

        private void unlock(RpcResponse rpcResponse){
            this.rpcResponse = rpcResponse;
            long time = (System.currentTimeMillis() - beginTime );

            if(time >= 5) {
                System.out.println("rpc调用时间过长 = " + time);
            }
            synchronized (RpcLock.this){
                notify();
            }
        }
    }

    /**
     * 客户端实例配置
     */
    private class RpcInstanceConfig {
        int timeout;
        String serviceName;
        private RpcInstanceConfig(int timeout, String serviceName) {
            this.timeout = timeout;
            this.serviceName = serviceName;
        }
    }
}
