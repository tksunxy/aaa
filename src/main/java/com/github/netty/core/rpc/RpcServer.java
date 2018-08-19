package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.util.ExceptionUtil;
import io.netty.channel.*;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by acer01 on 2018/8/18/018.
 */
public class RpcServer extends AbstractNettyServer{

    private RpcSessionServerHandler handler = new RpcSessionServerHandler();

    public RpcServer(int port) {
        super(port);
    }

    public RpcServer(String preName, int port) {
        super(preName,new InetSocketAddress(port));
    }

    public RpcServer(InetSocketAddress address) {
        super(address);
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
                pipeline.addLast(handler);
            }
        };
    }

    public void addService(String serviceName,Object service){
        handler.addService(serviceName,service);
    }

    @ChannelHandler.Sharable
    private class RpcSessionServerHandler extends AbstractChannelHandler<RpcRequest> {
        private Map<String,RpcService> serviceMap = new HashMap<>();

        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
            String serviceName = rpcRequest.getServiceName();
            String methodName = rpcRequest.getMethodName();

            Object result = null;
            int status;
            String message;

            RpcService rpcService = serviceMap.get(serviceName);
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

        public void addService(String serviceName, Object service){
            Object oldService = serviceMap.put(serviceName,new RpcService(serviceName,service));
            if(oldService != null){
                throw new RuntimeException("service exist ["+serviceName+"]");
            }
        }
    }

}
