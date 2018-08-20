package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.ReflectUtil;
import io.netty.channel.*;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by acer01 on 2018/8/18/018.
 */
public class RpcServer extends AbstractNettyServer{

    private RpcServerHandler rpcServerHandler = new RpcServerHandler();

    public RpcServer(int port) {
        this("",port);
    }

    public RpcServer(String preName, int port) {
        this(preName,new InetSocketAddress(port));
    }

    public RpcServer(String preName,InetSocketAddress address) {
        super(preName,address);
        this.rpcServerHandler = newRpcServerHandler();
    }

    protected RpcServerHandler newRpcServerHandler(){
        RpcServerHandler rpcServerHandler = new RpcServerHandler();
        //默认开启rpc基本命令服务
        rpcServerHandler.addService(new RpcCommandServiceImpl());
        return rpcServerHandler;
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<Channel>() {

            private ObjectEncoder encoder = new ObjectEncoder();

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast(encoder);
                pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                pipeline.addLast(rpcServerHandler);
            }
        };
    }

    public void addService(Object service){
        rpcServerHandler.addService(service);
    }

    @ChannelHandler.Sharable
    protected class RpcServerHandler extends AbstractChannelHandler<RpcRequest> {
        private final Map<String,RpcService> serviceInstanceMap = new HashMap<>();
        private Map<String,Channel> channelMap = new ConcurrentHashMap<>();

        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
            String serviceName = rpcRequest.getServiceName();
            String methodName = rpcRequest.getMethodName();
            logger.info(serviceName+"--"+methodName);

            Object result = null;
            int status;
            String message;

            RpcService rpcService = serviceInstanceMap.get(serviceName);
            if(rpcService == null){
                status = RpcResponse.NO_SUCH_SERVICE;
                message = "not found service ["+ serviceName +"]";
            }else {
                try {
                    result = rpcService.invoke(methodName, rpcRequest.getParametersVal());
                    status = RpcResponse.OK;
                    message = "ok";
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    status = RpcResponse.NO_SUCH_METHOD;
                    message = e.getMessage();
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    status = RpcResponse.SERVER_ERROR;
                    message = e.getTargetException().getMessage();
                    e.printStackTrace();
                }
            }

            RpcResponse response = new RpcResponse(status,message);
            response.setRequestId(rpcRequest.getRequestId());
            response.setData(result);
            ctx.writeAndFlush(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            ExceptionUtil.printRootCauseStackTrace(cause);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            Channel channel = ctx.channel();

            InetSocketAddress remoteAddress = getInetSocketAddress(channel.remoteAddress());
            if(remoteAddress == null){
                return;
            }

            channelMap.put(remoteAddress.getHostString() + ":" + remoteAddress.getPort(),channel);
            logger.info("新入链接 = "+ctx.channel().toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            logger.info("断开链接" + ctx.channel().toString());
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

        /**
         * 增加服务
         * @param service
         */
        public void addService(Object service){
            RpcInterface rpcInterfaceAnn = findRpcInterfaceAnn(service);
            if (rpcInterfaceAnn == null) {
                //缺少RpcInterface注解
                throw new IllegalStateException("The class is not exist Annotation, you must coding @RpcInterface");
            }

            String serviceName = rpcInterfaceAnn.value();

            synchronized (serviceInstanceMap) {
                Object oldService = serviceInstanceMap.get(serviceName);
                if (oldService != null) {
                    throw new IllegalStateException("The service exist [" + serviceName + "]");
                }
                serviceInstanceMap.put(serviceName, new RpcService(serviceName, service));
            }
        }

        private InetSocketAddress getInetSocketAddress(SocketAddress socketAddress){
            if(socketAddress instanceof InetSocketAddress){
                return (InetSocketAddress) socketAddress;
            }
            return null;
        }
    }

}
