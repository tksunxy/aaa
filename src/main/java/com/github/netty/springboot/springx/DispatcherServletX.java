package com.github.netty.springboot.springx;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.*;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by acer01 on 2018/8/29/029.
 */
public class DispatcherServletX extends DispatcherServlet {

    public DispatcherServletX() {
        super();
    }

    public DispatcherServletX(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    @Override
    public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
        super.setDetectAllHandlerMappings(detectAllHandlerMappings);
    }

    @Override
    public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
        super.setDetectAllHandlerAdapters(detectAllHandlerAdapters);
    }

    @Override
    public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
        super.setDetectAllHandlerExceptionResolvers(detectAllHandlerExceptionResolvers);
    }

    @Override
    public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
        super.setDetectAllViewResolvers(detectAllViewResolvers);
    }

    @Override
    public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
        super.setThrowExceptionIfNoHandlerFound(throwExceptionIfNoHandlerFound);
    }

    @Override
    public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
        super.setCleanupAfterInclude(cleanupAfterInclude);
    }

    @Override
    protected void onRefresh(ApplicationContext context) {
        super.onRefresh(context);
    }

    @Override
    protected void initStrategies(ApplicationContext context) {
        super.initStrategies(context);
    }

    @Override
    protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
        return super.getDefaultStrategy(context, strategyInterface);
    }

    @Override
    protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
        return super.getDefaultStrategies(context, strategyInterface);
    }

    @Override
    protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
        return super.createDefaultStrategy(context, clazz);
    }

    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.doService(request, response);
    }

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.doDispatch(request, response);
    }

    @Override
    protected LocaleContext buildLocaleContext(HttpServletRequest request) {
        return super.buildLocaleContext(request);
    }

    @Override
    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        return super.checkMultipart(request);
    }

    @Override
    protected void cleanupMultipart(HttpServletRequest request) {
        super.cleanupMultipart(request);
    }

    @Override
    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        return super.getHandler(request);
    }

    @Override
    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.noHandlerFound(request, response);
    }

    @Override
    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        return super.getHandlerAdapter(handler);
    }

    @Override
    protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        return super.processHandlerException(request, response, handler, ex);
    }

    @Override
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.render(mv, request, response);
    }

    @Override
    protected String getDefaultViewName(HttpServletRequest request) throws Exception {
        return super.getDefaultViewName(request);
    }

    @Override
    protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {
        return super.resolveViewName(viewName, model, locale, request);
    }

    @Override
    public void setContextAttribute(String contextAttribute) {
        super.setContextAttribute(contextAttribute);
    }

    @Override
    public String getContextAttribute() {
        return super.getContextAttribute();
    }

    @Override
    public void setContextClass(Class<?> contextClass) {
        super.setContextClass(contextClass);
    }

    @Override
    public Class<?> getContextClass() {
        return super.getContextClass();
    }

    @Override
    public void setContextId(String contextId) {
        super.setContextId(contextId);
    }

    @Override
    public String getContextId() {
        return super.getContextId();
    }

    @Override
    public void setNamespace(String namespace) {
        super.setNamespace(namespace);
    }

    @Override
    public String getNamespace() {
        return super.getNamespace();
    }

    @Override
    public void setContextConfigLocation(String contextConfigLocation) {
        super.setContextConfigLocation(contextConfigLocation);
    }

    @Override
    public String getContextConfigLocation() {
        return super.getContextConfigLocation();
    }

    @Override
    public void setContextInitializers(ApplicationContextInitializer<?>... initializers) {
        super.setContextInitializers(initializers);
    }

    @Override
    public void setContextInitializerClasses(String contextInitializerClasses) {
        super.setContextInitializerClasses(contextInitializerClasses);
    }

    @Override
    public void setPublishContext(boolean publishContext) {
        super.setPublishContext(publishContext);
    }

    @Override
    public void setPublishEvents(boolean publishEvents) {
        super.setPublishEvents(publishEvents);
    }

    @Override
    public void setThreadContextInheritable(boolean threadContextInheritable) {
        super.setThreadContextInheritable(threadContextInheritable);
    }

    @Override
    public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
        super.setDispatchOptionsRequest(dispatchOptionsRequest);
    }

    @Override
    public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
        super.setDispatchTraceRequest(dispatchTraceRequest);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
    }

    @Override
    protected WebApplicationContext initWebApplicationContext() {
        return super.initWebApplicationContext();
    }

    @Override
    protected WebApplicationContext findWebApplicationContext() {
        return super.findWebApplicationContext();
    }

    @Override
    protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
        return super.createWebApplicationContext(parent);
    }

    @Override
    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
        super.configureAndRefreshWebApplicationContext(wac);
    }

    @Override
    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
        return super.createWebApplicationContext(parent);
    }

    @Override
    protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
        super.postProcessWebApplicationContext(wac);
    }

    @Override
    protected void applyInitializers(ConfigurableApplicationContext wac) {
        super.applyInitializers(wac);
    }

    @Override
    public String getServletContextAttributeName() {
        return super.getServletContextAttributeName();
    }

    @Override
    protected void initFrameworkServlet() throws ServletException {
        super.initFrameworkServlet();
    }

    @Override
    public void refresh() {
        super.refresh();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        super.onApplicationEvent(event);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doOptions(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doTrace(request, response);
    }

    @Override
    protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        return super.buildRequestAttributes(request, response, previousAttributes);
    }

    @Override
    protected String getUsernameForRequest(HttpServletRequest request) {
        return super.getUsernameForRequest(request);
    }

    @Override
    public void setEnvironment(Environment environment) {
        super.setEnvironment(environment);
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return super.getEnvironment();
    }

    @Override
    protected ConfigurableEnvironment createEnvironment() {
        return super.createEnvironment();
    }

    @Override
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
        super.initBeanWrapper(bw);
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        return super.getLastModified(req);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doHead(req, resp);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        super.service(req, res);
    }


}
