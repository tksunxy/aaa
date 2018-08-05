package com.github.netty.core;

import com.github.netty.core.support.PartialPooledByteBufAllocator;
import com.github.netty.util.ExceptionUtil;
import com.github.netty.util.HostUtil;
import com.github.netty.util.NamespaceUtil;
import com.github.netty.util.ProxyUtil;
import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;


/**
 * 一个抽象的netty服务端
 * @author 84215
 */
public abstract class AbstractNettyServer implements Runnable{

    private String name;
    private ServerBootstrap bootstrap;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private ChannelFactory<?extends ServerChannel> channelFactory;
    private ChannelInitializer<?extends Channel> initializerChannelHandler;
    private ChannelFuture closeFuture;
    private Channel serverChannel;
    private InetSocketAddress socketAddress;
    private boolean enableEpoll;

    public AbstractNettyServer(int port) {
        this(new InetSocketAddress(port));
    }

    public AbstractNettyServer(InetSocketAddress address) {
        super();
        this.enableEpoll = HostUtil.isLinux() && Epoll.isAvailable();
        this.socketAddress = address;
        this.name = NamespaceUtil.newIdName(this.getClass(),"nettyServer");
        this.bootstrap = newServerBootstrap();
        this.boss = newBossEventLoopGroup();
        this.worker = newWorkerEventLoopGroup();
        this.channelFactory = newServerChannelFactory();
        this.initializerChannelHandler = newInitializerChannelHandler();
    }


    protected abstract ChannelInitializer<?extends Channel> newInitializerChannelHandler();

    protected ServerBootstrap newServerBootstrap(){
        return new NettyServerBootstrap();
    }

    protected EventLoopGroup newWorkerEventLoopGroup() {
        EventLoopGroup worker;
        int nEventLoopCount = Runtime.getRuntime().availableProcessors() * 2;
        if(enableEpoll){
            worker = new EpollEventLoopGroup(nEventLoopCount);
        }else {
            NioEventLoopWorkerGroup jdkWorker = new NioEventLoopWorkerGroup(nEventLoopCount);
            worker = ProxyUtil.newProxyByJdk(jdkWorker, jdkWorker.toString(), true);
        }
        return worker;
    }

    protected EventLoopGroup newBossEventLoopGroup() {
        EventLoopGroup boss;
        if(enableEpoll){
            EpollEventLoopGroup epollBoss = new EpollEventLoopGroup(1);
            epollBoss.setIoRatio(100);
            boss = epollBoss;
        }else {
            NioEventLoopBossGroup jdkBoss = new NioEventLoopBossGroup(1);
            jdkBoss.setIoRatio(100);
            boss = ProxyUtil.newProxyByJdk(jdkBoss, jdkBoss.toString(), true);
        }
        return boss;
    }

    protected ChannelFactory<? extends ServerChannel> newServerChannelFactory() {
        ChannelFactory<? extends ServerChannel> channelFactory;
        if(enableEpoll){
            channelFactory = EpollServerSocketChannel::new;
        }else {
            ChannelFactory<NioServerSocketChannel> serverChannelFactory = new NioServerChannelFactory();

            channelFactory = ProxyUtil.newProxyByJdk(serverChannelFactory, serverChannelFactory.toString(), true);
        }
        return channelFactory;
    }

    @Override
    public final void run() {
        try {
            bootstrap
                    .group(boss, worker)
                    .channelFactory(channelFactory)
                    .childHandler(initializerChannelHandler)
                    //允许在同一端口上启动同一服务器的多个实例，只要每个实例捆绑一个不同的本地IP地址即可
                    .option(ChannelOption.SO_REUSEADDR, true)
                    //用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
//                    .option(ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued

                    //禁用Nagle算法，即数据包立即发送出去 (在TCP_NODELAY模式下，假设有3个小包要发送，第一个小包发出后，接下来的小包需要等待之前的小包被ack，在这期间小包会合并，直到接收到之前包的ack后才会发生)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    //开启TCP/IP协议实现的心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, false)
                    //netty的默认内存分配器
                    .childOption(ChannelOption.ALLOCATOR, PartialPooledByteBufAllocator.INSTANCE);
//                    .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

            ChannelFuture channelFuture = bootstrap.bind(socketAddress);
            //堵塞
            channelFuture.await();
            //唤醒后获取异常
            Throwable cause = channelFuture.cause();

            startAfter(cause);

            //没异常就 堵塞住close的回调
            if(cause == null) {
                serverChannel = channelFuture.channel();
                closeFuture = serverChannel.closeFuture();
                closeFuture.sync();
            }
        } catch (Throwable throwable) {
            ExceptionUtil.printRootCauseStackTrace(throwable);
        }
    }

    public void stop() {
        try {
            boss.shutdownGracefully().sync();
            worker.shutdownGracefully().sync();
            serverChannel.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if(closeFuture != null) {
                closeFuture.notify();
            }
        }
    }

    public int getPort() {
        if(socketAddress == null){
            return 0;
        }
        return socketAddress.getPort();
    }

    protected void startAfter(Throwable cause){
        //有异常抛出
        if(cause != null){
            PlatformDependent.throwException(cause);
        }
        System.out.println("netty serverStart["+getPort()+"]...");
    }

    @Override
    public String toString() {
        return name+"{" +
                "port=" + getPort() +
                '}';
    }

}
