package com.github.netty.rpc;

import com.github.netty.core.AbstractNettyServer;
import com.github.netty.rpc.codec.RpcProto;
import com.github.netty.rpc.service.RpcCommandServiceImpl;
import com.github.netty.rpc.service.RpcDBServiceImpl;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;

/**
 * rpc服务端
 * @author acer01
 *  2018/8/18/018
 */
public class RpcServer extends AbstractNettyServer{

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
        rpcServerHandler.addInstance(new RpcCommandServiceImpl());
        //默认开启DB服务
        rpcServerHandler.addInstance(new RpcDBServiceImpl());
        return rpcServerHandler;
    }

    /**
     * 增加实例class (不能是接口,抽象类)
     * @param service
     */
    public void addInstance(Object service){
        rpcServerHandler.addInstance(service);
    }

    /**
     * 初始化所有处理器
     * @return
     */
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

}
