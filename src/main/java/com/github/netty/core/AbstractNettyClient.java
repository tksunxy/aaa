package com.github.netty.core;

import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.HostUtil;
import com.github.netty.core.util.NamespaceUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import com.github.netty.core.util.Logger;
import com.github.netty.core.util.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 一个抽象的netty客户端
 *
 * @author acer01
 *  2018/8/18/018
 */
public abstract class AbstractNettyClient implements Runnable{

    protected Logger logger;
    private String name;
    private Bootstrap bootstrap;

    private EventLoopGroup worker;
    private ChannelFactory<?extends Channel> channelFactory;
    private ChannelInitializer<?extends Channel> initializerChannelHandler;
    private InetSocketAddress remoteAddress;
    private boolean enableEpoll;
    private SocketChannel socketChannel;

    public AbstractNettyClient(String remoteHost,int remotePort) {
        this(new InetSocketAddress(remoteHost,remotePort));
    }

    public AbstractNettyClient(InetSocketAddress remoteAddress) {
        this("",remoteAddress);
    }

    /**
     *
     * @param namePre 名称前缀
     * @param remoteAddress 远程地址
     */
    public AbstractNettyClient(String namePre,InetSocketAddress remoteAddress) {
        super();
        this.enableEpoll = HostUtil.isLinux() && Epoll.isAvailable();
        this.remoteAddress = remoteAddress;
        this.name = namePre + NamespaceUtil.newIdName(getClass());
        this.bootstrap = newClientBootstrap();
        this.worker = newWorkerEventLoopGroup();
        this.channelFactory = newClientChannelFactory();
        this.initializerChannelHandler = newInitializerChannelHandler();
        this.logger = LoggerFactory.getLogger(getClass());
    }


    protected abstract ChannelInitializer<?extends Channel> newInitializerChannelHandler();

    protected Bootstrap newClientBootstrap(){
        return new Bootstrap();
    }

    protected EventLoopGroup newWorkerEventLoopGroup() {
        EventLoopGroup worker;
        int nEventLoopCount = 1;
        if(enableEpoll){
            worker = new EpollEventLoopGroup(nEventLoopCount);
        }else {
            worker = new NioEventLoopWorkerGroup(nEventLoopCount);
        }
        return worker;
    }

    protected ChannelFactory<? extends Channel> newClientChannelFactory() {
        ChannelFactory<? extends Channel> channelFactory;
        if(enableEpoll){
            channelFactory = EpollSocketChannel::new;
        }else {
            channelFactory = NioSocketChannel::new;
        }
        return channelFactory;
    }

    @Override
    public final void run() {
        try {
            bootstrap
                    .group(worker)
                    .channelFactory(channelFactory)
                    .handler(initializerChannelHandler)
                    .remoteAddress(remoteAddress)
                    //用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
//                    .option(ChannelOption.SO_BACKLOG, 1024) // determining the number of connections queued

                    //禁用Nagle算法，即数据包立即发送出去 (在TCP_NODELAY模式下，假设有3个小包要发送，第一个小包发出后，接下来的小包需要等待之前的小包被ack，在这期间小包会合并，直到接收到之前包的ack后才会发生)
                    .option(ChannelOption.TCP_NODELAY, true)
                    //开启TCP/IP协议实现的心跳机制
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    //netty的默认内存分配器
                    .option(ChannelOption.ALLOCATOR, PartialPooledByteBufAllocator.INSTANCE);
//                    .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

            connect();
            startAfter();
        } catch (Throwable throwable) {
            logger.error(throwable.getMessage());
//            ExceptionUtil.printRootCauseStackTrace(throwable);
        }
    }

    public boolean isConnect(){
        if(socketChannel == null){
            return false;
        }

        try {
            ChannelFuture future = socketChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).sync();
            return future.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean connect(){
        try {
            ChannelFuture channelFuture = bootstrap.connect().sync();
            if(!channelFuture.isSuccess()){
                return false;
            }

            socketChannel = (SocketChannel) channelFuture.channel();
            return true;
        } catch (Exception e) {
            Throwable root = ExceptionUtil.getRootCauseNotNull(e);
            logger.error("Connect fail "+remoteAddress +"  : ["+ root.toString()+"]");
            return false;
        }
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void stop() {
        Throwable cause = null;
        try {
            if(socketChannel != null) {
                socketChannel.shutdown();
            }

        } catch (Exception e) {
            cause = e;
        }
        stopAfter(cause);
    }

    protected void stopAfter(Throwable cause){
        //有异常抛出
        if(cause != null){
            ExceptionUtil.printRootCauseStackTrace(cause);
        }

        logger.info(name + " stop [port = "+getPort()+"]...");
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        if(socketChannel == null){
            return 0;
        }
        return socketChannel.localAddress().getPort();
    }

    protected void startAfter(){
        InetSocketAddress address = getRemoteAddress();

        logger.info(name + " start [port = "+getPort()+", remoteAddress = "+address.getHostName()+":"+address.getPort()+"]...");
    }

    @Override
    public String toString() {
        InetSocketAddress address = getRemoteAddress();
        return name+"{" +
                "port=" + getPort() +
                ", remoteAddress=" + address.getHostName()+ ":" + address.getPort()+
                '}';
    }
}
