package com.github.netty.rpc;

/**
 * Created by acer01 on 2018/9/16/016.
 */

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.constants.CoreConstants;
import com.github.netty.core.util.ReflectUtil;
import com.github.netty.rpc.codec.DataCodec;
import com.github.netty.rpc.codec.JsonDataCodec;
import com.github.netty.rpc.codec.RpcProto;
import com.github.netty.rpc.codec.RpcResponseStatus;
import com.github.netty.rpc.exception.RpcResponseException;
import com.github.netty.rpc.exception.RpcTimeoutException;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.collection.LongObjectHashMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * 客户端处理器
 */
@ChannelHandler.Sharable
public class RpcClientHandler extends AbstractChannelHandler<RpcProto.Response> {

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
     * 请求锁
     */
    private final Map<Long,RpcLock> requestLockMap = new LongObjectHashMap<>(64);
    /**
     * 实例
     */
    private final Map<String,RpcInstance> instanceMap = new WeakHashMap<>();
    /**
     * 生成请求id
     */
    private final AtomicLong requestIdIncr = new AtomicLong();
    /**
     * 数据编码解码器
     */
    private DataCodec dataCodec = new JsonDataCodec();
    /**
     * 获取链接
     */
    private Supplier<SocketChannel> channelSupplier;

    public RpcClientHandler(Supplier<SocketChannel> channelSupplier) {
        this.channelSupplier = Objects.requireNonNull(channelSupplier);
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, RpcProto.Response rpcResponse) throws Exception {
        if(CoreConstants.isEnableExecuteHold()) {
            CoreConstants.holdExecute(() -> {
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

    /**
     * 新建请求id
     * @return
     */
    protected long newRequestId(){
        return requestIdIncr.incrementAndGet();
    }

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
     * 客户端实例
     */
    private class RpcInstance implements InvocationHandler {
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
            channelSupplier.get().writeAndFlush(rpcRequest);

            TOTAL_INVOKE_COUNT.incrementAndGet();
            //上锁, 等待服务端响应释放锁
            RpcProto.Response rpcResponse = lock.lock(timeout,TimeUnit.MILLISECONDS);
            //移除锁
            requestLockMap.remove(requestId);

            if(rpcResponse == null){
                if(CoreConstants.isEnableExecuteHold()) {
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
     * 远程调用后,等待响应的同步锁
     */
    public static class RpcLock{

        public long beginTime;
        private volatile RpcProto.Response rpcResponse;
        private Thread lockThread;
        public static AtomicLong TOTAL_SPIN_RESPONSE_COUNT = new AtomicLong();

        private RpcProto.Response lock(int timeout,TimeUnit timeUnit) throws InterruptedException {
            this.lockThread = Thread.currentThread();
            this.beginTime = System.currentTimeMillis();

            //自旋, 因为如果是本地rpc调用,速度太快了, 没必要再堵塞
            int spinCount = CoreConstants.getRpcLockSpinCount();
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

}