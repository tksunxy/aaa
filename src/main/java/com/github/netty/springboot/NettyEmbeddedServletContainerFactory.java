package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletDefaultHttpServlet;
import com.github.netty.servlet.ServletSessionCookieConfig;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.springframework.boot.context.embedded.*;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;

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
            return container;
        }catch (Exception e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * 新建servlet上下文
     * @return
     */
    private ServletContext newServletContext(){
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();
        ExecutorService asyncExecutorService = newAsyncExecutorService();

        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(new URL[]{}, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);

        servletContext.setAsyncExecutorService(asyncExecutorService);
        return servletContext;
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

    protected ExecutorService newAsyncExecutorService(){
//        return Executors.newFixedThreadPool(bizThreadCount);
        return new DefaultEventExecutorGroup(50);
//        return null;
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
