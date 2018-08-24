package com.github.netty.core.rpc;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.rpc.codec.DataCodec;
import com.github.netty.core.rpc.codec.JsonDataCodec;
import com.github.netty.core.rpc.codec.RpcProto;
import com.github.netty.core.rpc.codec.RpcResponseStatus;
import com.github.netty.core.rpc.service.RpcCommandServiceImpl;
import com.github.netty.core.rpc.service.RpcDBServiceImpl;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.ReflectUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * rpc服务端
 * @author acer01
 *  2018/8/18/018
 */
public class RpcServer extends AbstractNettyServer{

    /**
     * 数据编码解码器
     */
    private DataCodec dataCodec = new JsonDataCodec();
    /**
     * rpc服务端处理器
     */
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
        //默认开启DB服务
        rpcServerHandler.addService(new RpcDBServiceImpl());
        return rpcServerHandler;
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<Channel>() {
            MessageToByteEncoder<ByteBuf> varintEncoder = new ProtobufVarint32LengthFieldPrepender();
            MessageToMessageDecoder<ByteBuf> protobufDecoder = new ProtobufDecoder(RpcProto.Request.getDefaultInstance());
            MessageToMessageEncoder<MessageLiteOrBuilder> protobufEncoder = new ProtobufEncoder();

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                ByteToMessageDecoder varintDecoder = new ProtobufVarint32FrameDecoder();
                pipeline.addLast(varintDecoder);
                pipeline.addLast(protobufDecoder);
                pipeline.addLast(varintEncoder);
                pipeline.addLast(protobufEncoder);
                pipeline.addLast(rpcServerHandler);
            }
        };
    }

    public void addService(Object service){
        rpcServerHandler.addService(service);
    }

    public DataCodec getDataCodec() {
        return dataCodec;
    }

    @ChannelHandler.Sharable
    protected class RpcServerHandler extends AbstractChannelHandler<RpcProto.Request> {
        private final Map<String,RpcService> serviceInstanceMap = new HashMap<>();
        private Map<String,Channel> channelMap = new ConcurrentHashMap<>();

        @Override
        protected void onMessageReceived(ChannelHandlerContext ctx, RpcProto.Request rpcRequest) throws Exception {
            String serviceName = rpcRequest.getServiceName();
            String methodName = rpcRequest.getMethodName();

            Object result = null;
            int status;
            String message;

            RpcService rpcService = serviceInstanceMap.get(serviceName);
            if(rpcService == null){
                status = RpcResponseStatus.NO_SUCH_SERVICE;
                message = "not found service ["+ serviceName +"]";
            }else {
                try {
                    byte[] requestDataBytes = rpcRequest.getData().toByteArray();
                    Object[] requestData = dataCodec.decodeRequestData(requestDataBytes);
                    result = rpcService.invoke(methodName, requestData);

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

            logger.info(serviceName+"--"+methodName+"--["+status+"]--"+result+"");

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

            Channel channel = ctx.channel();
            InetSocketAddress remoteAddress = getInetSocketAddress(channel.remoteAddress());
            if(remoteAddress == null){
                return;
            }
            channelMap.remove(remoteAddress.getHostString() + ":" + remoteAddress.getPort(),channel);
            logger.info("断开链接" + ctx.channel().toString());
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
                serviceInstanceMap.put(serviceName, new RpcService(serviceName, service,RpcServer.this));
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
    }

}
