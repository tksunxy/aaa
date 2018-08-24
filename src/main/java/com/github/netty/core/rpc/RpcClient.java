package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyClient;
import com.github.netty.core.rpc.codec.DataCodec;
import com.github.netty.core.rpc.codec.JsonDataCodec;
import com.github.netty.core.rpc.codec.RpcResponseStatus;
import com.github.netty.core.rpc.codec.RpcProto;
import com.github.netty.core.rpc.exception.RpcConnectException;
import com.github.netty.core.rpc.exception.RpcException;
import com.github.netty.core.rpc.exception.RpcResponseException;
import com.github.netty.core.rpc.exception.RpcTimeoutException;
import com.github.netty.core.rpc.service.RpcCommandService;
import com.github.netty.core.rpc.service.RpcDBService;
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    private static RpcScheduledThreadPoolExecutor SCHEDULE_SERVICE;
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
    private final Map<Long,RpcLock> requestLockMap = new HashMap<>();
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
        this("",address);
    }

    public RpcClient(String namePre, InetSocketAddress remoteAddress) {
        super(namePre, remoteAddress);
        run();
    }

    /**
     * 开启自动重连
     * @param heartIntervalSecond 心跳检测间隔
     * @param timeUnit 时间单位
     * @param reconnectSuccessHandler 重连成功后
     */
    public void enableAutoReconnect(int heartIntervalSecond, TimeUnit timeUnit, Consumer<SocketChannel> reconnectSuccessHandler){
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
    public void enableAutoReconnect(Consumer<SocketChannel> reconnectSuccessHandler){
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
    protected Long newRequestId(){
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
     * 客户端处理器
     */
    @ChannelHandler.Sharable
    private class RpcClientHandler extends AbstractChannelHandler<RpcProto.Response> {
        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcProto.Response rpcResponse) throws Exception {
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
    private class RpcHeartbeatTask implements Runnable{
        private Consumer<SocketChannel> reconnectSuccessHandler;
        /**
         * 重连次数
         */
        private int reconnectCount;
        /**
         * 最大超时重试次数
         */
        private int maxTimeoutRetryNum = 3;

        private RpcHeartbeatTask(Consumer<SocketChannel> reconnectSuccessHandler) {
            this.reconnectSuccessHandler = reconnectSuccessHandler;
        }

        private boolean reconnect(String causeMessage){
            int count = ++reconnectCount;
            boolean success = connect();

            if (success) {
                reconnectCount = 0;
                SocketChannel socketChannel = getSocketChannel();

                logger.info("第[" + count + "]次断线重连 : 成功" + socketChannel +", 重连原因["+ causeMessage +"]");
                if(reconnectSuccessHandler != null){
                    reconnectSuccessHandler.accept(socketChannel);
                }

            } else {
                logger.info("第[" + count + "]次断线重连 : 失败, 重连原因["+ causeMessage +"]");
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
                e.printStackTrace();
            }
        }
    }

    /**
     * 远程调用后,等待响应的同步锁
     */
    private class RpcLock{
        private long beginTime;
        private RpcProto.Response rpcResponse;

        private RpcProto.Response lock(int timeout) throws InterruptedException {
            this.beginTime = System.currentTimeMillis();
            synchronized (RpcLock.this){
                wait(timeout);
            }

            return rpcResponse;
        }

        private void unlock(RpcProto.Response rpcResponse){
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
            Long requestId = newRequestId();
            byte[] requestDataBytes = dataCodec.encodeRequestData(args);

            RpcProto.Request request = RpcProto.Request.newBuilder()
                    .setRequestId(requestId)
                    .setServiceName(serviceName)
                    .setMethodName(methodName)
                    .setData(ByteString.copyFrom(requestDataBytes))
                    .build();

            getSocketChannel().writeAndFlush(request);

            RpcLock lock = new RpcLock();
            requestLockMap.put(requestId,lock);
            //上锁, 等待服务端响应释放锁
            RpcProto.Response rpcResponse = lock.lock(timeout);
            //移除锁
            requestLockMap.remove(requestId);

            if(rpcResponse == null){
                throw new RpcTimeoutException("requestTimeout : maxTimeout is ["+timeout+"]");
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
     * 获取线程调度执行器, 注: 延迟创建
     * @return
     */
    private static RpcScheduledThreadPoolExecutor getScheduleService() {
        if(SCHEDULE_SERVICE == null){
            synchronized (RpcClient.class){
                if(SCHEDULE_SERVICE == null){
                    SCHEDULE_SERVICE = new RpcScheduledThreadPoolExecutor(1);
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
