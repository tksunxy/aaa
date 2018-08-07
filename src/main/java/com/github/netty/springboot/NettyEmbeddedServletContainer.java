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
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {

    private ServletContext servletContext;
    private boolean enableSsl;
    private SslContext sslContext;
    private ChannelHandler servletDispatcherHandler;
    private ChannelHandler servletCodecHandler;
    private final Thread serverThread;

    @TodoOptimize("ssl没测试能不能用")
    public NettyEmbeddedServletContainer(ServletContext servletContext,Ssl ssl,int bizThreadCount) throws SSLException {
        super(servletContext.getServerSocketAddress());
        this.servletContext = servletContext;
        this.servletCodecHandler = new NettyServletCodecHandler(servletContext);
        ExecutorService dispatcherExecutor = newDispatcherExecutor(bizThreadCount);
        this.servletDispatcherHandler = new NettyServletDispatcherHandler(dispatcherExecutor);
        this.serverThread = new Thread(this);

        configServerThread();
        initSsl(ssl);
    }

    private ExecutorService newDispatcherExecutor(int bizThreadCount){
//        return Executors.newFixedThreadPool(bizThreadCount);
        return new DefaultEventExecutorGroup(bizThreadCount);
//        return null;
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (enableSsl) {
                    SSLEngine engine = sslContext.newEngine(ch.alloc());
                    //是否客户端
                    engine.setUseClientMode(false);
                    pipeline.addLast("SSL", new SslHandler(engine,true));
                }


                //HTTP编码解码
                pipeline.addLast("HttpCodec", new HttpServerCodec(4096, 8192, 8192, false));

                //HTTP聚合，设置最大消息值为512KB
                pipeline.addLast("Aggregator", new HttpObjectAggregator(512 * 1024));

                //内容压缩
                pipeline.addLast("ContentCompressor", new HttpContentCompressor());
//                pipeline.addLast("ContentDecompressor", new HttpContentDecompressor());

                //分段写入, 防止响应数据过大
//                pipeline.addLast("ChunkedWrite",new ChunkedWriteHandler());

                //生成servletRequest和servletResponse对象
                pipeline.addLast("ServletCodec",servletCodecHandler);

                //业务调度器, 让对应的Servlet处理请求
                pipeline.addLast("ServletDispatcherDispatcher", servletDispatcherHandler);
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

    private void configServerThread() throws SSLException {
        serverThread.setName(servletContext.getServerInfo());
        serverThread.setUncaughtExceptionHandler((thread,throwable)->{
            //
        });
    }

    private void initSsl(Ssl ssl) throws SSLException {
        this.enableSsl = ssl != null && ssl.isEnabled();
        if(enableSsl){
            this.sslContext = newSslContext(ssl);
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
