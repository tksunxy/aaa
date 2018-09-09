package com.github.netty.springboot;

import com.github.netty.ContainerConfig;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.support.NettyThreadX;
import com.github.netty.core.util.TodoOptimize;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletFilterRegistration;
import com.github.netty.servlet.ServletRegistration;
import com.github.netty.servlet.support.ServletEventListenerManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * netty容器
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainer implements EmbeddedServletContainer {

    private final ServletContext servletContext;

    private ServletServer servletServer;

    /**
     * 服务器地址
     */
    private InetSocketAddress servletServerAddress;

    private ContainerConfig config;

    public NettyEmbeddedServletContainer(ServletContext servletContext,Ssl ssl,ContainerConfig config) throws SSLException {
        this.servletContext = servletContext;
        this.servletServerAddress = servletContext.getServletServerAddress();
        this.config = config;
        this.servletServer = new ServletServer(servletServerAddress,ssl);
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextInitialized(new ServletContextEvent(servletContext));
        }

        servletServer.start();
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextDestroyed(new ServletContextEvent(servletContext));
        }

        servletServer.stop();
    }

    @Override
    public int getPort() {
        return servletServerAddress.getPort();
    }

    public class ServletServer extends AbstractNettyServer{
        private final Thread servletServerThread;
        private boolean enableSsl;
        private SslContext sslContext;

        @TodoOptimize("ssl没测试能不能用")
        private ServletServer(InetSocketAddress address, Ssl ssl) throws SSLException {
            super("Netty",address);

            setIoRatio(config.getServerIoRatio());
            setWorkerCount(config.getServerWorkerCount());
            this.servletServerThread = new NettyThreadX(this,getName());
            initSsl(ssl);
        }

        public void start(){
            servletServerThread.setUncaughtExceptionHandler((thread, throwable)->{
                //
            });
            servletServerThread.start();
        }

        @Override
        protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
            return new ChannelInitializer<SocketChannel>() {
                private ChannelHandler servletHandler = new NettyServletHandler(servletContext,config.getServerHandlerExecutor());

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
//                    pipeline.addLast("ContentCompressor", new HttpContentCompressor());
//                pipeline.addLast("ContentDecompressor", new HttpContentDecompressor());

                    //分段写入, 防止响应数据过大
//                pipeline.addLast("ChunkedWrite",new ChunkedWriteHandler());

                    //业务调度器, 让对应的Servlet处理请求
                    pipeline.addLast("ServletHandler", servletHandler);
                }
            };
        }

        @Override
        public void stop() {
            destroyFilter();
            destroyServlet();
            super.stop();
        }

        private void destroyFilter(){
            Map<String, ServletFilterRegistration> servletRegistrationMap = servletContext.getFilterRegistrations();
            for(Map.Entry<String,ServletFilterRegistration> entry : servletRegistrationMap.entrySet()){
                ServletFilterRegistration registration = entry.getValue();
                Filter filter = registration.getFilter();
                filter.destroy();
            }
        }

        private void destroyServlet(){
            Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
            for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
                ServletRegistration registration = entry.getValue();
                Servlet servlet = registration.getServlet();
                servlet.destroy();
            }
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
}
