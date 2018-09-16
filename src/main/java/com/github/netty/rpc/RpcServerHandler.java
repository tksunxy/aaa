package com.github.netty.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.ReflectUtil;
import com.github.netty.rpc.codec.DataCodec;
import com.github.netty.rpc.codec.JsonDataCodec;
import com.github.netty.rpc.codec.RpcProto;
import com.github.netty.rpc.codec.RpcResponseStatus;
import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by acer01 on 2018/9/16/016.
 */
@ChannelHandler.Sharable
public class RpcServerHandler extends AbstractChannelHandler<RpcProto.Request> {

    /**
     * 数据编码解码器
     */
    private DataCodec dataCodec = new JsonDataCodec();
    private final Map<String,RpcInstance> serviceInstanceMap = new HashMap<>();
    private Map<String,Channel> channelMap = new ConcurrentHashMap<>();

    @Override
    protected void onMessageReceived(ChannelHandlerContext ctx, RpcProto.Request rpcRequest) throws Exception {
        String serviceName = rpcRequest.getServiceName();
        String methodName = rpcRequest.getMethodName();

        Object result = null;
        int status;
        String message;

        RpcInstance rpcInstance = serviceInstanceMap.get(serviceName);
        if(rpcInstance == null){
            status = RpcResponseStatus.NO_SUCH_SERVICE;
            message = "not found service ["+ serviceName +"]";
        }else {
            try {
                Object[] requestData = dataCodec.decodeRequestData(rpcRequest.getData().toStringUtf8());
                result = rpcInstance.invoke(methodName, requestData);

                status = RpcResponseStatus.OK;
                message = "ok";
            } catch (NoSuchMethodException | IllegalAccessException e) {
                status = RpcResponseStatus.NO_SUCH_METHOD;
                message = e.getMessage();
            } catch (Throwable e) {
                status = RpcResponseStatus.SERVER_ERROR;
                message = e.getCause() == null? e.toString() : e.getCause().getMessage();
            }
        }

        //是否进行编码
        int isEncode;
        byte[] responseDataBytes;
        if(result instanceof byte[]){
            isEncode = 0;
            responseDataBytes = (byte[]) result;
        }else {
            isEncode = 1;
            responseDataBytes = dataCodec.encodeResponseData(result);
        }

        RpcProto.Response rpcResponse = RpcProto.Response.newBuilder()
                .setRequestId(rpcRequest.getRequestId())
                .setStatus(status)
                .setMessage(message)
                .setEncode(isEncode)
                .setData(ByteString.copyFrom(responseDataBytes))
                .build();

        ctx.writeAndFlush(rpcResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ExceptionUtil.printRootCauseStackTrace(cause);
        removeChannel(ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        putChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        removeChannel(ctx.channel());
    }

    /**
     * 放入链接
     * @param channel
     */
    private void putChannel(Channel channel){
        InetSocketAddress remoteAddress = getInetSocketAddress(channel.remoteAddress());
        if(remoteAddress == null){
            return;
        }
        channelMap.put(remoteAddress.getHostString() + ":" + remoteAddress.getPort(),channel);
        logger.info("新入链接 = "+channel.toString());
    }

    /**
     * 移除链接
     * @param channel
     */
    private void removeChannel(Channel channel){
        InetSocketAddress remoteAddress = getInetSocketAddress(channel.remoteAddress());
        if(remoteAddress == null){
            return;
        }
        channelMap.remove(remoteAddress.getHostString() + ":" + remoteAddress.getPort(),channel);
        logger.info("断开链接" + channel.toString());
    }

    /**
     * 增加实例
     * @param instance
     */
    public void addInstance(Object instance){
        RpcInterface rpcInterfaceAnn = findRpcInterfaceAnn(instance);
        if (rpcInterfaceAnn == null) {
            //缺少RpcInterface注解
            throw new IllegalStateException("The class is not exist Annotation, you must coding @RpcInterface");
        }

        String serviceName = rpcInterfaceAnn.value();
        int timeout = rpcInterfaceAnn.timeout();

        synchronized (serviceInstanceMap) {
            Object oldService = serviceInstanceMap.get(serviceName);
            if (oldService != null) {
                throw new IllegalStateException("The service exist [" + serviceName + "]");
            }
            serviceInstanceMap.put(serviceName, new RpcInstance(serviceName, timeout,instance));
        }
    }

    private RpcInterface findRpcInterfaceAnn(Object service){
        Class[] interfaces = ReflectUtil.getInterfaces(service);
        for(Class i : interfaces){
            RpcInterface rpcInterfaceAnn = ReflectUtil.findAnnotation(i, RpcInterface.class);
            if(rpcInterfaceAnn != null){
                return rpcInterfaceAnn;
            }
        }
        return null;
    }

    private InetSocketAddress getInetSocketAddress(SocketAddress socketAddress){
        if(socketAddress instanceof InetSocketAddress){
            return (InetSocketAddress) socketAddress;
        }
        return null;
    }

    private class RpcInstance {
        private int timeout;
        private String serviceName;
        private Object instance;
        private List<Method> methodList;

        private RpcInstance(String serviceName, int timeout, Object instance) {
            this.serviceName = serviceName;
            this.timeout = timeout;
            this.instance = instance;

            List<Class> interfaceList = getInterfaceList(instance);
            if(interfaceList.isEmpty()){
                throw new RuntimeException("rpc服务必须至少拥有一个接口");
            }

            this.methodList = getMethodList(interfaceList);
            if(methodList.isEmpty()){
                throw new RuntimeException("rpc服务接口必须至少拥有一个方法");
            }
        }

        public Object invoke(String methodName,Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            int argsCount = args == null? 0: args.length;
            Method method = getMethod(methodName,argsCount);
            if(method == null){
                throw new NoSuchMethodException("not found method ["+methodName+"]");
            }

            if(argsCount > 0) {
                checkTypeAutoCast(method.getParameterTypes(),args);
            }

            Object result = method.invoke(instance,args);
            return result;
        }

        /**
         * 检查参数类型并自动转换
         * @param types 类型
         * @param args 参数
         */
        private void checkTypeAutoCast(Class<?>[] types,Object[] args){
            DataCodec rpcDataCodec = dataCodec;
            int size = types.length;
            for (int i = 0; i < size; i++) {
                Object arg = args[i];
                Class type = types[i];

                //type 所对应类信息是arg对象所对应的类信息的父类或者是父接口，简单理解即type是arg的父类或接口
                if(!type.isAssignableFrom(arg.getClass())){
                    args[i] = rpcDataCodec.cast(arg, type);
                }
            }
        }

        public Method getMethod(String methodName,int argsCount) {
            if(methodList instanceof RandomAccess) {
                int size = methodList.size();
                for (int i=0; i<size; i++){
                    Method method = methodList.get(i);
                    if(method.getName().equals(methodName) && method.getParameterCount() == argsCount){
                        return method;
                    }
                }
            }else {
                for(Method method : methodList){
                    if(method.getName().equals(methodName) && method.getParameterCount() == argsCount){
                        return method;
                    }
                }
            }
            return null;
        }

        private List<Method> getMethodList(List<Class> interfaceList){
            List<Method> methodList = new ArrayList<>();
            for(Class interfaceClazz : interfaceList) {
                methodList.addAll(Arrays.asList(interfaceClazz.getMethods()));
            }
            return methodList;
        }

        private List<Class> getInterfaceList(Object source){
            List<Class> interfaceList = new ArrayList<>();
            Class sourceClass = source.getClass();
            for(Class currClass = sourceClass; currClass != null; currClass = currClass.getSuperclass()){
                interfaceList.addAll(Arrays.asList(currClass.getInterfaces()));
            }
            return interfaceList;
        }
    }
}
