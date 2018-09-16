package com.github.netty.rpc;

import com.github.netty.core.AbstractNettyClient;
import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.rpc.codec.RpcProto;
import com.github.netty.rpc.exception.RpcConnectException;
import com.github.netty.rpc.exception.RpcException;
import com.github.netty.rpc.exception.RpcTimeoutException;
import com.github.netty.rpc.service.RpcCommandService;
import com.github.netty.rpc.service.RpcDBService;
import com.google.protobuf.MessageLiteOrBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * rpc客户端
 * @author acer01
 *  2018/8/18/018
 */
public class RpcClient extends AbstractNettyClient{

    /**
     * 调度线程池
     */
    private static ThreadPoolX SCHEDULE_POOL;
    /**
     * rpc客户端处理器
     */
    private RpcClientHandler rpcClientHandler = new RpcClientHandler(this::getSocketChannel);
    /**
     * rpc命令服务
     */
    private RpcCommandService rpcCommandService;
    /**
     * rpc数据服务
     */
    private RpcDBService rpcDBService;
    /**
     * 连接状态
     */
    private State state;

    public RpcClient(String remoteHost, int remotePort) {
        this(new InetSocketAddress(remoteHost, remotePort));
    }

    public RpcClient(InetSocketAddress address) {
        this("",address);
    }

    public RpcClient(String namePre, InetSocketAddress remoteAddress) {
        super(namePre, remoteAddress);
    }

    /**
     * 开启自动重连
     */
    public void enableAutoReconnect(){
        enableAutoReconnect(10,TimeUnit.SECONDS,null);
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
            throw new IllegalStateException("if enableAutoReconnect, you must start client and has a commandService");
        }
        RpcHeartbeatTask heartbeatTask = new RpcHeartbeatTask(reconnectSuccessHandler);
        getSchedulePool().scheduleWithFixedDelay(heartbeatTask,heartIntervalSecond,heartIntervalSecond,timeUnit);
    }

    /**
     * 新建实例
     * @param clazz 接口 interface
     * @param <T>
     * @return  实例
     */
    public <T>T newInstance(Class<T> clazz){
        return rpcClientHandler.newInstance(clazz);
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
     * 获取线程调度执行器, 注: 延迟创建
     * @return
     */
    public static ThreadPoolX getSchedulePool() {
        if(SCHEDULE_POOL == null){
            synchronized (RpcClient.class){
                if(SCHEDULE_POOL == null){
                    SCHEDULE_POOL = new ThreadPoolX("RpcClient",1);
                }
            }
        }
        return SCHEDULE_POOL;
    }

    /**
     * 获取数据服务
     * @return
     */
    public RpcDBService getRpcDBService() {
        if(rpcDBService == null){
            synchronized (this) {
                if(rpcDBService == null) {
                    rpcDBService = newInstance(RpcDBService.class);
                }
            }
        }
        return rpcDBService;
    }

    /**
     * 获取命令服务
     * @return
     */
    public RpcCommandService getRpcCommandService() {
        if(rpcCommandService == null){
            synchronized (this) {
                if(rpcCommandService == null) {
                    rpcCommandService = newInstance(RpcCommandService.class);
                }
            }
        }
        return rpcCommandService;
    }

    /**
     * 获取连接状态
     * @return
     */
    public State getState() {
        return state;
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
                logger.info(RpcClient.this.getName() + " 心跳包 : " + new String(msg));

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

    /**
     * 客户端连接状态
     */
    public enum State{
        DOWN,
        UP
    }

    @Override
    public String toString() {
        return super.toString()+"{" +
                "state=" + state +
                '}';
    }

}
