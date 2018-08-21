package com.github.netty.springboot;

import com.github.netty.servlet.*;
import com.github.netty.session.RemoteCommandServer;
import com.github.netty.session.SessionService;
import com.github.netty.session.impl.CompositeSessionServiceImpl;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.springframework.boot.context.embedded.*;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.net.ssl.SSLException;
import javax.servlet.ServletException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * netty容器工厂
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {

    protected ResourceLoader resourceLoader;

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        try {
            ServletContext servletContext = newServletContext();
            NettyEmbeddedServletContainer container = newNettyEmbeddedServletContainer(servletContext);

            //默认 servlet
            if (isRegisterDefaultServlet()) {
                registerDefaultServlet(servletContext);
            }

            //jsp servlet
            JspServlet jspServlet = getJspServlet();
            if(jspServlet != null  && jspServlet.getRegistered()){
                registerJspServlet(servletContext,jspServlet);
            }

            //初始化
            for (ServletContextInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }

            initFilter(servletContext);
            initServlet(servletContext);
            return container;
        }catch (Exception e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * 初始化过滤器
     * @param servletContext
     */
    private void initFilter(ServletContext servletContext){
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

    /**
     * 初始化servlet
     * @param servletContext
     */
    private void initServlet(ServletContext servletContext){
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

    /**
     * 新建servlet上下文
     * @return
     */
    private ServletContext newServletContext(){
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();

        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(new URL[]{}, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);

        servletContext.setSessionService(newSessionService());
        logger.info("NewInstance "+SessionService.class.getSimpleName()+" using ["+servletContext.getSessionService()+"]");

        servletContext.setAsyncExecutorSupplier(newAsyncExecutorSupplier());
        return servletContext;
    }

    /**
     * 新建会话服务
     * @return
     */
    protected SessionService newSessionService(){
        //组合会话
        CompositeSessionServiceImpl compositeSessionService = new CompositeSessionServiceImpl();

        //远程会话地址
        InetSocketAddress remoteSessionServerAddress = new InetSocketAddress(getPort() + 1);
        //远程命令启动服务
        RemoteCommandServer remoteCommandServer = new RemoteCommandServer(remoteSessionServerAddress);
        remoteCommandServer.execLocalCommand("cmd", result ->{
            if(result.isSuccess()) {
                System.out.println(result.getMessage());
                compositeSessionService.enableRemoteSession(remoteSessionServerAddress);
            }
        });
        return compositeSessionService;
    }

    /**
     * 注册默认servlet
     * @param servletContext servlet上下文
     */
    protected void registerDefaultServlet(ServletContext servletContext){
        ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
        servletContext.addServlet("default",defaultServlet);
    }

    /**
     * 注册 jsp servlet
     * @param servletContext servlet上下文
     */
    protected void registerJspServlet(ServletContext servletContext,JspServlet jspServlet){

    }

    /**
     * 新建netty容器
     * @param servletContext servlet上下文
     * @return netty容器
     * @throws SSLException ssl异常
     */
    protected NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext) throws SSLException {
        Ssl ssl = getSsl();
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext,ssl);
        return container;
    }

    protected Supplier<ExecutorService> newAsyncExecutorSupplier(){
        return new Supplier<ExecutorService>() {
            private ExecutorService executorService;

            @Override
            public ExecutorService get() {
                if(executorService == null) {
                    synchronized (this){
                        if(executorService == null) {
//                            executorService = Executors.newFixedThreadPool(8);
                            executorService = new DefaultEventExecutorGroup(15);
                        }
                    }
                }
                return executorService;
            }
        };
    }

    /**
     * 加载session的cookie配置
     * @return cookie配置
     */
    protected ServletSessionCookieConfig loadSessionCookieConfig(){
        ServletSessionCookieConfig sessionCookieConfig = new ServletSessionCookieConfig();
        sessionCookieConfig.setMaxAge(-1);
        //session超时时间
        sessionCookieConfig.setSessionTimeout(getSessionTimeout());
        return sessionCookieConfig;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
