package com.github.netty.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyClient;
import com.github.netty.rpc.codec.DataCodec;
import com.github.netty.rpc.codec.JsonDataCodec;
import com.github.netty.rpc.codec.RpcProto;
import com.github.netty.rpc.codec.RpcResponseStatus;
import com.github.netty.rpc.exception.RpcConnectException;
import com.github.netty.rpc.exception.RpcException;
import com.github.netty.rpc.exception.RpcResponseException;
import com.github.netty.rpc.exception.RpcTimeoutException;
import com.github.netty.rpc.service.RpcCommandService;
import com.github.netty.rpc.service.RpcDBService;
import com.github.netty.OptimizeConfig;
import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.core.util.ReflectUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.collection.LongObjectHashMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * rpc客户端
 * @author acer01
 *  2018/8/18/018
 */
public class RpcClient extends AbstractNettyClient{

    /**
     * 线程调度执行器
     */
    private static ThreadPoolX SCHEDULE_SERVICE;
    /**
     * 不需要代理的方法
     */
    private static final Collection<String> NO_PROXY_METHOD_LIST;
    static {
        NO_PROXY_METHOD_LIST = new HashSet<>();
        for(Method method : Object.class.getMethods()){
            NO_PROXY_METHOD_LIST.add(method.getName());
        }
    }

    /**
     * 数据编码解码器
     */
    private DataCodec dataCodec = new JsonDataCodec();
    /**
     * rpc客户端处理器
     */
    private ChannelHandler rpcClientHandler = new RpcClientHandler();
    /**
     * 连接状态
     */
    private State state;
    /**
     * rpc命令服务
     */
    private RpcCommandService rpcCommandService;
    /**
     * rpc数据服务
     */
    private RpcDBService rpcDBService;
    /**
     * 请求锁
     */
    private final Map<Long,RpcLock> requestLockMap = new LongObjectHashMap<>(64);
    /**
     * 生成请求id
     */
    private final AtomicLong requestIdIncr = new AtomicLong();
    /**
     * 实例
     */
    private final Map<String,RpcInstance> instanceMap = new WeakHashMap<>();
    /**
     * 方法重写
     */
    private final Map<String,RpcOverrideMethod> methodOverrideMap = new HashMap<>();

    public RpcClient(String remoteHost, int remotePort) {
        this(new InetSocketAddress(remoteHost, remotePort));
    }

    public RpcClient(InetSocketAddress address) {
        this("",address,0);
    }

    public RpcClient(String namePre, InetSocketAddress remoteAddress,int socketChannelCount) {
        super(namePre, remoteAddress, socketChannelCount);
        run();
    }

    /**
     * 开启自动重连
     * @param heartIntervalSecond 心跳检测间隔
     * @param timeUnit 时间单位
     * @param reconnectSuccessHandler 重连成功后
     */
    public void enableAutoReconnect(int heartIntervalSecond, TimeUnit timeUnit, Consumer<RpcClient> reconnectSuccessHandler){
        if(rpcCommandService == null){
            //自动重连依赖命令服务
            throw new IllegalStateException("if enableAutoReconnect, you must has commandService");
        }
        RpcHeartbeatTask heartbeatTask = new RpcHeartbeatTask(reconnectSuccessHandler);
        getScheduleService().scheduleWithFixedDelay(heartbeatTask,heartIntervalSecond,heartIntervalSecond,timeUnit);
    }

    /**
     * 开启自动重连
     * @param reconnectSuccessHandler 重连成功后
     */
    public void enableAutoReconnect(Consumer<RpcClient> reconnectSuccessHandler){
        enableAutoReconnect(10,TimeUnit.SECONDS,reconnectSuccessHandler);
    }

    /**
     * 添加方法重写
     * @param methodName 需要重写的方法名
     * @param method 执行方法
     * @return 旧的方法
     */
    public RpcOverrideMethod addOverrideMethod(String methodName, RpcOverrideMethod method){
        return methodOverrideMap.put(methodName,method);
    }

    /**
     * 获取连接状态
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * 新建请求id
     * @return
     */
    protected long newRequestId(){
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

        String serviceName = rpcInterfaceAnn.value();
        int timeout = rpcInterfaceAnn.timeout();
        RpcInstance rpcInstance = new RpcInstance(timeout, serviceName);

        T instance = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, rpcInstance);
        instanceMap.put(serviceName,rpcInstance);
        return instance;
    }

    /**
     * 初始化所有处理器
     * @return
     */
    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<Channel>() {
            MessageToByteEncoder<ByteBuf> varintEncoder = new ProtobufVarint32LengthFieldPrepender();
            MessageToMessageDecoder<ByteBuf> protobufDecoder = new ProtobufDecoder(RpcProto.Response.getDefaultInstance());
            MessageToMessageEncoder<MessageLiteOrBuilder> protobufEncoder = new ProtobufEncoder();

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                ByteToMessageDecoder varintDecoder = new ProtobufVarint32FrameDecoder();
                pipeline.addLast(varintDecoder);
                pipeline.addLast(protobufDecoder);
                pipeline.addLast(varintEncoder);
                pipeline.addLast(protobufEncoder);
                pipeline.addLast(rpcClientHandler);
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


    @Override
    public boolean isConnect() {
        if(rpcCommandService == null){
            return super.isConnect();
        }

        SocketChannel channel = super.getSocketChannel();
        if(channel == null){
            return false;
        }
        try {
            return rpcCommandService.ping() != null;
        }catch (RpcException e){
            return false;
        }
    }

    @Override
    protected void startAfter() {
        super.startAfter();
        rpcCommandService = newInstance(RpcCommandService.class);
        rpcDBService = newInstance(RpcDBService.class);
    }

    public RpcDBService getRpcDBService() {
        return rpcDBService;
    }

    public RpcCommandService getRpcCommandService() {
        return rpcCommandService;
    }

    @Override
    public boolean connect() {
        boolean success = super.connect();
        if(success){
            state = State.UP;
        }else {
            state = State.DOWN;
        }
        return success;
    }

    /**
     * 心跳任务
     */
    private class RpcHeartbeatTask implements Runnable{
        private Consumer<RpcClient> reconnectSuccessHandler;
        /**
         * 重连次数
         */
        private int reconnectCount;
        /**
         * 最大超时重试次数
         */
        private int maxTimeoutRetryNum = 3;

        private RpcHeartbeatTask(Consumer<RpcClient> reconnectSuccessHandler) {
            this.reconnectSuccessHandler = reconnectSuccessHandler;
        }

        private boolean reconnect(String causeMessage){
            int count = ++reconnectCount;
            boolean success = connect();

            logger.info("第[" + count + "]次断线重连 :" + (success?"成功! 共保持"+getSocketChannelCount()+"个连接":"失败") +", 重连原因["+ causeMessage +"]");
            if (success) {
                reconnectCount = 0;
                if(reconnectSuccessHandler != null){
                    reconnectSuccessHandler.accept(RpcClient.this);
                }
            }
            return success;
        }

        @Override
        public void run() {
            try {
                byte[] msg = rpcCommandService.ping();
                logger.info("心跳包 : " + new String(msg));

            }catch (RpcConnectException e) {
                reconnect(e.getMessage());

            }catch (RpcTimeoutException e){
                //重ping N次, 如果N次后还ping不通, 则进行重连
                for(int i = 0; i< maxTimeoutRetryNum; i++) {
                    try {
                        byte[] msg = rpcCommandService.ping();
                        return;
                    } catch (RpcConnectException e1) {
                        reconnect(e1.getMessage());
                        return;
                    }catch (RpcTimeoutException e2){
                        //
                    }
                }
                reconnect(e.getMessage());
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
        }
    }

    //总调用次数
    private static AtomicLong TOTAL_INVOKE_COUNT = new AtomicLong();
    //超时api
    private static Map<String,Integer> TIMEOUT_API = new ConcurrentHashMap<>();

    public static String getTimeoutApis() {
        return String.join(",", TIMEOUT_API.keySet());
    }

    public static long getTotalInvokeCount() {
        return TOTAL_INVOKE_COUNT.get();
    }

    public static long getTotalTimeoutCount() {
        return TIMEOUT_API.values().stream().reduce(0,Integer::sum);
    }

    /**
     * 远程调用后,等待响应的同步锁
     */
    public static class RpcLock{
        private long beginTime;
        private volatile RpcProto.Response rpcResponse;
        private Thread lockThread;
        public static AtomicLong TOTAL_SPIN_RESPONSE_COUNT = new AtomicLong();

        private RpcProto.Response lock(int timeout,TimeUnit timeUnit) throws InterruptedException {
            this.lockThread = Thread.currentThread();
            this.beginTime = System.currentTimeMillis();

            //自旋, 因为如果是本地rpc调用,速度太快了, 没必要再堵塞
            int spinCount = OptimizeConfig.getRpcLockSpinCount();
            for (int i=0; rpcResponse == null && i<spinCount; i++){
                //
            }

            //如果自旋后拿到响应 直接返回
            if(rpcResponse != null){
                TOTAL_SPIN_RESPONSE_COUNT.incrementAndGet();
                return rpcResponse;
            }

            //没有拿到响应, 则堵塞
            LockSupport.parkNanos(timeUnit.toNanos(timeout));
            if(rpcResponse != null){
                return rpcResponse;
            }
            return null;
        }

        private void unlock(RpcProto.Response rpcResponse){
            this.rpcResponse = rpcResponse;
            LockSupport.unpark(lockThread);
        }
    }

    /**
     * 客户端实例
     */
    private class RpcInstance implements InvocationHandler{
        private int timeout;
        private String serviceName;
        private RpcInstance(int timeout, String serviceName) {
            this.timeout = timeout;
            this.serviceName = serviceName;
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
            //重写的方法
            RpcOverrideMethod overrideMethod = methodOverrideMap.get(methodName);
            if(overrideMethod != null){
                return overrideMethod.invoke(proxy,method,args);
            }

            //不代理的方法
            if(NO_PROXY_METHOD_LIST.contains(methodName)){
                return method.invoke(RpcInstance.this,args);
            }

            //其他方法
            long requestId = newRequestId();
            byte[] requestDataBytes = dataCodec.encodeRequestData(args);

            RpcProto.Request rpcRequest = RpcProto.Request.newBuilder()
                    .setRequestId(requestId)
                    .setServiceName(serviceName)
                    .setMethodName(methodName)
                    .setData(ByteString.copyFrom(requestDataBytes))
                    .build();


            RpcLock lock = new RpcLock();
            requestLockMap.put(requestId,lock);
            getSocketChannel().writeAndFlush(rpcRequest);

            TOTAL_INVOKE_COUNT.incrementAndGet();
            //上锁, 等待服务端响应释放锁
            RpcProto.Response rpcResponse = lock.lock(timeout,TimeUnit.MILLISECONDS);
            //移除锁
            requestLockMap.remove(requestId);

            if(rpcResponse == null){
                if(OptimizeConfig.isEnableExecuteHold()) {
                    logger.error("超时的请求 : " + rpcRequest);
                }

                TIMEOUT_API.put(methodName,TIMEOUT_API.getOrDefault(methodName,0) + 1);
                throw new RpcTimeoutException("RequestTimeout : serviceName = ["+serviceName+"], methodName=["+methodName+"], maxTimeout = ["+timeout+"]");
            }

            int status = rpcResponse.getStatus();
            //400以上的状态都是错误状态
            if(status >= RpcResponseStatus.NO_SUCH_METHOD){
                throw new RpcResponseException(status,rpcResponse.getMessage());
            }

            byte[] responseDataBytes = rpcResponse.getData().toByteArray();
            //如果服务器进行编码了, 就解码
            if(rpcResponse.getEncode() == 1) {
                return dataCodec.decodeResponseData(responseDataBytes);
            }else {
                return responseDataBytes;
            }
        }

        @Override
        public String toString() {
            return "RpcInstance{" +
                    "serviceName='" + serviceName + '\'' +
                    '}';
        }
    }

    /**
     * 客户端处理器
     */
    @ChannelHandler.Sharable
    private class RpcClientHandler extends AbstractChannelHandler<RpcProto.Response> {
        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcProto.Response rpcResponse) throws Exception {
            if(OptimizeConfig.isEnableExecuteHold()) {
                OptimizeConfig.holdExecute(() -> {
                    RpcLock lock = requestLockMap.remove(rpcResponse.getRequestId());
                    //如果获取不到锁 说明已经超时, 被释放了
                    if (lock == null) {
                        logger.error("-----------------------!!严重"+rpcResponse);
                        return;
                    }

                    long out = System.currentTimeMillis() - lock.beginTime;
                    if(out > 10) {
                        logger.error("超时的响应[" +
                                out +
                                "] :" + rpcResponse);
                    }
                    lock.unlock(rpcResponse);
                });
                return;
            }

            RpcLock lock = requestLockMap.get(rpcResponse.getRequestId());
            //如果获取不到锁 说明已经超时, 被释放了
            if (lock == null) {
                return ;
            }
            lock.unlock(rpcResponse);
        }
    }

    @Override
    public String toString() {
        return super.toString()+"{" +
                "state=" + state +
                '}';
    }

    /**
     * 获取线程调度执行器, 注: 延迟创建
     * @return
     */
    private static ThreadPoolX getScheduleService() {
        if(SCHEDULE_SERVICE == null){
            synchronized (RpcClient.class){
                if(SCHEDULE_SERVICE == null){
                    SCHEDULE_SERVICE = new ThreadPoolX("Rpc",1);
                }
            }
        }
        return SCHEDULE_SERVICE;
    }

    /**
     * 客户端连接状态
     */
    public enum State{
        DOWN,
        UP
    }
}
