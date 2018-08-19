package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyClient;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by acer01 on 2018/8/18/018.
 */
public class RpcClient extends AbstractNettyClient implements InvocationHandler {

    //请求锁
    private final Map<Integer,Lock> requestLockMap = new HashMap<>();
    //请求id生成
    private final AtomicInteger requestIdIncr = new AtomicInteger();
    //服务映射
    private Map<Class,String> serviceMap = new HashMap<>();
    //超时时间 (毫秒)
    private int timeout;
    //不代理Object类的方法
    Collection<String> noProxyMethodList = new HashSet<>();{
        for(Method method : Object.class.getMethods()){
            noProxyMethodList.add(method.getName());
        }
    }
    //默认超时时间 (20毫秒)
    public static final int DEFAULT_TIME_OUT = 20;

    public RpcClient(String remoteHost, int remotePort) {
        this(new InetSocketAddress(remoteHost, remotePort));
    }

    public RpcClient(InetSocketAddress address) {
        this("",address);
    }

    public RpcClient(String namePre, InetSocketAddress remoteAddress) {
        super(namePre, remoteAddress);
        this.timeout = DEFAULT_TIME_OUT;
        run();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public <T>T newInstance(String serviceName, Class<T> clazz){
        serviceMap.put(clazz,serviceName);
        T instance = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz},this);
        return instance;
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<Channel>() {
            private ChannelHandler handler = new RpcSessionClientHandler();
            private ObjectEncoder encoder = new ObjectEncoder();

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast(new IdleStateHandler(20,10,0));
                pipeline.addLast(encoder);
                pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                pipeline.addLast(handler);
            }
        };
    }

    @Override
    public SocketChannel getSocketChannel() {
        SocketChannel socketChannel = super.getSocketChannel();
        if(socketChannel == null){
            throw new RuntimeException("客户端未连接上服务端");
        }
        return socketChannel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if(noProxyMethodList.contains(methodName)){
            return method.invoke(this,args);
        }

        Integer requestId = newRequestId();
        String serviceName = serviceMap.get(method.getDeclaringClass());

        RpcRequest rpcRequest = new RpcRequest(requestId);
        rpcRequest.setServiceName(serviceName);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParametersVal(args);

        ChannelFuture channelFuture = getSocketChannel().writeAndFlush(rpcRequest);

        Lock lock = new Lock();
        requestLockMap.put(requestId,lock);
        //上锁, 等待服务端响应释放锁
        lock.lock(timeout);
        //移除锁
        requestLockMap.remove(requestId);

        RpcResponse rpcResponse = lock.getRpcResponse();
        if(rpcResponse != null && rpcResponse.getStatus() == RpcResponse.OK){
            return rpcResponse.getData();
        }
        return null;
    }

    private Integer newRequestId(){
        return requestIdIncr.incrementAndGet();
//        return UUID.randomUUID().toString();
    }

    @ChannelHandler.Sharable
    private class RpcSessionClientHandler extends AbstractChannelHandler<RpcResponse> {
        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
            Lock lock = requestLockMap.get(rpcResponse.getRequestId());
            //如果获取不到锁 说明已经超时, 被释放了
            if(lock == null){
                return;
            }
            lock.unlock(rpcResponse);
        }
    }

    private class Lock{
        private long beginTime;
        private RpcResponse rpcResponse;

        public void lock(int timeout) throws InterruptedException {
            this.beginTime = System.currentTimeMillis();
            synchronized (Lock.this){
                wait(timeout);
            }
        }

        public void unlock(RpcResponse rpcResponse){
            this.rpcResponse = rpcResponse;
            long time = (System.currentTimeMillis() - beginTime );

            if(time >= 5) {
                System.out.println("rpc调用时间过长 = " + time);
            }
            synchronized (Lock.this){
                notify();
            }
        }

        public RpcResponse getRpcResponse() {
            return rpcResponse;
        }
    }

}
