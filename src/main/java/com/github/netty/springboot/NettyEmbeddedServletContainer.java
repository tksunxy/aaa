package com.github.netty.springboot;

import com.github.netty.core.AbstractNettyServer;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletFilterRegistration;
import com.github.netty.servlet.ServletRegistration;
import com.github.netty.servlet.support.ServletEventListenerManager;
import com.github.netty.util.TodoOptimize;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.io.File;
import java.util.Map;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {

    private ServletContext servletContext;
    private EventExecutorGroup dispatcherExecutorGroup;
    private final boolean enableSsl;
    private SslContext sslContext;
    private ChannelHandler dispatcherHandler;
    private final Thread serverThread;

    @TodoOptimize("ssl没测试能不能用")
    public NettyEmbeddedServletContainer(ServletContext servletContext,Ssl ssl,int bizThreadCount) throws SSLException {
        super(servletContext.getServerSocketAddress());

        this.servletContext = servletContext;
        this.enableSsl = ssl != null && ssl.isEnabled();
        if(enableSsl){
            this.sslContext = newSslContext(ssl);
        }
        this.dispatcherExecutorGroup = new DefaultEventExecutorGroup(bizThreadCount);
        this.dispatcherHandler = new NettyServletDispatcherHandler(servletContext);
        this.serverThread = new Thread(this);
        serverThread.setName(servletContext.getServerInfo());
        serverThread.setUncaughtExceptionHandler((thread,throwable)->{
            //
        });
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (enableSsl) {
                    SSLEngine engine = sslContext.newEngine(ch.alloc());
                    engine.setUseClientMode(false);//是否客户端
                    pipeline.addLast("SSL", new SslHandler(engine));
                }

                pipeline.addLast("HttpCodec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                pipeline.addLast("Aggregator", new HttpObjectAggregator(512 * 1024));  // HTTP聚合，设置最大消息值为512KB
                pipeline.addLast("ServletCodec",new NettyServletCodecHandler(servletContext) ); //处理请求，读入数据，生成Request和Response对象
                pipeline.addLast(dispatcherExecutorGroup, "Dispatcher", dispatcherHandler); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        };
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextInitialized(new ServletContextEvent(servletContext));
        }

        initFilter();
        initServlet();

        serverThread.start();
        System.out.println("启动成功 "+servletContext.getServerInfo()+"["+getPort()+"]...");
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextDestroyed(new ServletContextEvent(servletContext));
        }

        destroyFilter();
        destroyServlet();

        super.stop();
        synchronized (serverThread) {
            serverThread.interrupt();
        }
        System.out.println("停止成功 "+servletContext.getServerInfo()+"["+getPort()+"]...");
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    private void initFilter(){
        Map<String, ServletFilterRegistration> servletFilterRegistrationMap = servletContext.getFilterRegistrations();
        for(Map.Entry<String,ServletFilterRegistration> entry : servletFilterRegistrationMap.entrySet()){
            ServletFilterRegistration registration = entry.getValue();
            try {
                registration.getFilter().init(registration.getFilterConfig());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    private void initServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            try {
                registration.getServlet().init(registration.getServletConfig());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    private void destroyFilter(){
        Map<String, ServletFilterRegistration> servletRegistrationMap = servletContext.getFilterRegistrations();
        for(Map.Entry<String,ServletFilterRegistration> entry : servletRegistrationMap.entrySet()){
            ServletFilterRegistration registration = entry.getValue();
            registration.getFilter().destroy();
        }
    }

    private void destroyServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            registration.getServlet().destroy();
        }
    }

    private SslContext newSslContext(Ssl ssl) throws SSLException {
        File certChainFile = new File(ssl.getTrustStore());
        File keyFile = new File(ssl.getKeyStore());
        String keyPassword = ssl.getKeyPassword();

        SslContext sslContext = SslContext.newServerContext(certChainFile,keyFile,keyPassword);
        return sslContext;
    }

}
