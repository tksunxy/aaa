package com.github.netty.springboot.springx;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.*;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by acer01 on 2018/8/29/029.
 */
public class AnnotationConfigEmbeddedWebApplicationContextX extends AnnotationConfigEmbeddedWebApplicationContext {

    public AnnotationConfigEmbeddedWebApplicationContextX() {
        super();
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        super.setEnvironment(environment);
    }

    @Override
    public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
        super.setBeanNameGenerator(beanNameGenerator);
    }

    @Override
    public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
        super.setScopeMetadataResolver(scopeMetadataResolver);
    }

    @Override
    protected void prepareRefresh() {
        super.prepareRefresh();
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.postProcessBeanFactory(beanFactory);
    }

    @Override
    protected void onRefresh() {
        super.onRefresh();

    }

    @Override
    protected void finishRefresh() {
        super.finishRefresh();
    }

    @Override
    protected void onClose() {
        super.onClose();
    }

    @Override
    protected EmbeddedServletContainerFactory getEmbeddedServletContainerFactory() {
        return super.getEmbeddedServletContainerFactory();
    }

    @Override
    protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
        return super.getServletContextInitializerBeans();
    }

    @Override
    protected void prepareEmbeddedWebApplicationContext(ServletContext servletContext) {
        super.prepareEmbeddedWebApplicationContext(servletContext);
    }

    @Override
    protected Resource getResourceByPath(String path) {
        return super.getResourceByPath(path);
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
    public void setServletConfig(ServletConfig servletConfig) {
        super.setServletConfig(servletConfig);
    }

    @Override
    public ServletConfig getServletConfig() {
        return super.getServletConfig();
    }

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer() {
        return super.getEmbeddedServletContainer();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        super.setServletContext(servletContext);
    }

    @Override
    public ServletContext getServletContext() {
        return super.getServletContext();
    }

    @Override
    public String getApplicationName() {
        return super.getApplicationName();
    }

    @Override
    protected ConfigurableEnvironment createEnvironment() {
        return super.createEnvironment();
    }

    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        return super.getResourcePatternResolver();
    }

    @Override
    protected void initPropertySources() {
        super.initPropertySources();
    }

    @Override
    public Theme getTheme(String themeName) {
        return super.getTheme(themeName);
    }

    @Override
    public void setConfigLocation(String configLocation) {
        super.setConfigLocation(configLocation);
    }

    @Override
    public void setConfigLocations(String... configLocations) {
        super.setConfigLocations(configLocations);
    }

    @Override
    public String[] getConfigLocations() {
        return super.getConfigLocations();
    }

    @Override
    public void setParent(ApplicationContext parent) {
        super.setParent(parent);
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
        super.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
    }

    @Override
    public void setAllowCircularReferences(boolean allowCircularReferences) {
        super.setAllowCircularReferences(allowCircularReferences);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        super.setResourceLoader(resourceLoader);
    }

    @Override
    public Resource getResource(String location) {
        return super.getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return super.getResources(locationPattern);
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        super.setClassLoader(classLoader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    @Override
    protected void cancelRefresh(BeansException ex) {
        super.cancelRefresh(ex);
    }

    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return super.getAutowireCapableBeanFactory();
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        super.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        super.removeBeanDefinition(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        return super.getBeanDefinition(beanName);
    }

    @Override
    public boolean isBeanNameInUse(String beanName) {
        return super.isBeanNameInUse(beanName);
    }

    @Override
    public void registerAlias(String beanName, String alias) {
        super.registerAlias(beanName, alias);
    }

    @Override
    public void removeAlias(String alias) {
        super.removeAlias(alias);
    }

    @Override
    public boolean isAlias(String beanName) {
        return super.isAlias(beanName);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setDisplayName(String displayName) {
        super.setDisplayName(displayName);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public ApplicationContext getParent() {
        return super.getParent();
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return super.getEnvironment();
    }

    @Override
    public long getStartupDate() {
        return super.getStartupDate();
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        super.publishEvent(event);
    }

    @Override
    public void publishEvent(Object event) {
        super.publishEvent(event);
    }

    @Override
    protected void publishEvent(Object event, ResolvableType eventType) {
        super.publishEvent(event, eventType);
    }

    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
        super.addBeanFactoryPostProcessor(postProcessor);
    }

    @Override
    public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return super.getBeanFactoryPostProcessors();
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        super.addApplicationListener(listener);
    }

    @Override
    public Collection<ApplicationListener<?>> getApplicationListeners() {
        return super.getApplicationListeners();
    }

    @Override
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        return super.obtainFreshBeanFactory();
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.prepareBeanFactory(beanFactory);
    }

    @Override
    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        super.invokeBeanFactoryPostProcessors(beanFactory);
    }

    @Override
    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        super.registerBeanPostProcessors(beanFactory);
    }

    @Override
    protected void initMessageSource() {
        super.initMessageSource();
    }

    @Override
    protected void initApplicationEventMulticaster() {
        super.initApplicationEventMulticaster();
    }

    @Override
    protected void initLifecycleProcessor() {
        super.initLifecycleProcessor();
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();
    }

    @Override
    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        super.finishBeanFactoryInitialization(beanFactory);
    }

    @Override
    protected void resetCommonCaches() {
        super.resetCommonCaches();
    }

    @Override
    public void registerShutdownHook() {
        super.registerShutdownHook();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void doClose() {
        super.doClose();
    }

    @Override
    protected void destroyBeans() {
        super.destroyBeans();
    }

    @Override
    public boolean isActive() {
        return super.isActive();
    }

    @Override
    protected void assertBeanFactoryActive() {
        super.assertBeanFactoryActive();
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return super.getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return super.getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return super.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return super.getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return super.getBean(requiredType, args);
    }

    @Override
    public boolean containsBean(String name) {
        return super.containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return super.isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return super.isPrototype(name);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return super.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return super.isTypeMatch(name, typeToMatch);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return super.getType(name);
    }

    @Override
    public String[] getAliases(String name) {
        return super.getAliases(name);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return super.containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return super.getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return super.getBeanDefinitionNames();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        return super.getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return super.getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        return super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return super.getBeansOfType(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        return super.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return super.getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        return super.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return super.findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    public BeanFactory getParentBeanFactory() {
        return super.getParentBeanFactory();
    }

    @Override
    public boolean containsLocalBean(String name) {
        return super.containsLocalBean(name);
    }

    @Override
    protected BeanFactory getInternalParentBeanFactory() {
        return super.getInternalParentBeanFactory();
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return super.getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return super.getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return super.getMessage(resolvable, locale);
    }

    @Override
    protected MessageSource getInternalParentMessageSource() {
        return super.getInternalParentMessageSource();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void addProtocolResolver(ProtocolResolver resolver) {
        super.addProtocolResolver(resolver);
    }

    @Override
    public Collection<ProtocolResolver> getProtocolResolvers() {
        return super.getProtocolResolvers();
    }
}