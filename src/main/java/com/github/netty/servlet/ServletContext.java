package com.github.netty.servlet;

import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;
import com.github.netty.core.util.RecyclableUtil;
import com.github.netty.core.util.TypeUtil;
import com.github.netty.servlet.support.MimeMappingsX;
import com.github.netty.servlet.support.ServletEventListenerManager;
import com.github.netty.servlet.support.UrlMapper;
import com.github.netty.session.SessionService;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 *
 * @author acer01
 *  2018/7/14/014
 */
public class ServletContext implements javax.servlet.ServletContext {

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());

    private Map<String,Object> attributeMap;
    private Map<String,String> initParamMap;

    private Map<String,ServletRegistration> servletRegistrationMap;
    private Map<String,ServletFilterRegistration> filterRegistrationMap;

    private Supplier<ExecutorService> asyncExecutorSupplier;

    private Set<SessionTrackingMode> sessionTrackingModeSet;

    private ServletEventListenerManager servletEventListenerManager;
    private ServletSessionCookieConfig sessionCookieConfig;
    private UrlMapper<Servlet> servletUrlMapper;
    private UrlMapper<Filter> filterUrlMapper;
    private String rootDirStr;
    private Charset defaultCharset;
    private InetSocketAddress servletServerAddress;
    private final String serverInfo;
    private final ClassLoader classLoader;
    private String contextPath;

    private SessionService sessionService;
    private MimeMappingsX mimeMappings;

    private int sessionTimeout;

    public ServletContext(InetSocketAddress socketAddress,
                          ClassLoader classLoader,
                          String contextPath, String serverInfo,
                          ServletSessionCookieConfig sessionCookieConfig, MimeMappingsX mimeMappings) {
//        File rootDir = new File("");
//        this.rootDirStr = rootDir.isAbsolute() ? rootDir.getAbsolutePath() : FilenameUtils.concat(new File(".").getAbsolutePath(), rootDir.getPath());
        this.sessionCookieConfig = Objects.requireNonNull(sessionCookieConfig);
        this.mimeMappings = Objects.requireNonNull(mimeMappings);
        this.serverInfo = serverInfo == null? "netty-server/1.0":serverInfo;

        this.contextPath = contextPath == null? "" : contextPath;
        this.defaultCharset = null;
        this.asyncExecutorSupplier = null;
        this.sessionTrackingModeSet = null;
        this.servletServerAddress = socketAddress;
        this.classLoader = classLoader;

        this.attributeMap = new HashMap<>(16);
        this.initParamMap = new HashMap<>(16);
        this.servletRegistrationMap = new HashMap<>(8);
        this.filterRegistrationMap = new HashMap<>(8);
        this.servletUrlMapper = new UrlMapper<>(contextPath,true);
        this.filterUrlMapper = new UrlMapper<>(contextPath,false);
        this.servletEventListenerManager = new ServletEventListenerManager();
        //默认20分钟
        this.sessionTimeout = 1200;
    }

    public MimeMappingsX getMimeMappings() {
        return mimeMappings;
    }

    public void setAsyncExecutorSupplier(Supplier<ExecutorService> asyncExecutorSupplier) {
        this.asyncExecutorSupplier = asyncExecutorSupplier;
    }

    public ExecutorService getAsyncExecutorService() {
        if(asyncExecutorSupplier == null){
            return null;
        }
        return asyncExecutorSupplier.get();
    }

    public ServletEventListenerManager getServletEventListenerManager() {
        return servletEventListenerManager;
    }

    public void addServletMapping(String urlPattern, String servletName, Servlet servlet) throws IllegalArgumentException {
        servletUrlMapper.addMapping(urlPattern, servlet, servletName);
    }

    public void addFilterMapping(String urlPattern, String filterName, Filter filter) throws IllegalArgumentException {
        filterUrlMapper.addMapping(urlPattern, filter, filterName);
    }

    public long getAsyncTimeout(){
        String value = getInitParameter("asyncTimeout");
        if(value == null){
            return 10000;
        }
        try {
            return Long.parseLong(value);
        }catch (NumberFormatException e){
            return 10000;
        }
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        if(sessionTimeout <= 0){
            return;
        }
        this.sessionTimeout = sessionTimeout;
    }

    public InetSocketAddress getServletServerAddress() {
        return servletServerAddress;
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public SessionService getSessionService() {
        return sessionService;
    }

    public Charset getDefaultCharset() {
        if(defaultCharset == null){
            defaultCharset = HttpConstants.DEFAULT_CHARSET;
        }
        return defaultCharset;
    }

    private List<Filter> matchFilterByPath(String path){
        return filterUrlMapper.getMappingObjectsByUri(path);
    }

    private List<Filter> matchFilterByName(String servletName){
        List<Filter> allNeedFilters = RecyclableUtil.newRecyclableList(10);

        for (ServletFilterRegistration registration : filterRegistrationMap.values()) {
            for(String name : registration.getServletNameMappings()){
                if(servletName.equals(name)){
                    allNeedFilters.add(registration.getFilter());
                }
            }
        }
        return allNeedFilters;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        if (file == null) {
            return null;
        }
        int period = file.lastIndexOf('.');
        if (period < 0) {
            return null;
        }
        String extension = file.substring(period + 1);
        if (extension.length() < 1) {
            return null;
        }
        return mimeMappings.get(extension);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        Set<String> thePaths = new HashSet<>();
        if (!path.endsWith("/")) {
            path += "/";
        }
        String basePath = getRealPath(path);
        if (basePath == null) {
            return thePaths;
        }
        File theBaseDir = new File(basePath);
        if (!theBaseDir.exists() || !theBaseDir.isDirectory()) {
            return thePaths;
        }
        String theFiles[] = theBaseDir.list();
        if (theFiles == null) {
            return thePaths;
        }
        for (String filename : theFiles) {
            File testFile = new File(basePath + File.separator + filename);
            if (testFile.isFile()) {
                thePaths.add(path + filename);
            } else if (testFile.isDirectory()) {
                thePaths.add(path + filename + "/");
            }
        }
        return thePaths;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        if (!path.startsWith("/")) {
            throw new MalformedURLException("Path '" + path + "' does not start with '/'");
        }
        URL url = new URL(getClassLoader().getResource(""), path.substring(1));
        try {
            url.openStream();
        } catch (Throwable t) {
            logger.warn("Throwing exception when getting InputStream of " + path + " in /");
            url = null;
        }
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getClassLoader().getResourceAsStream(path);
    }

    @Override
    public ServletRequestDispatcher getRequestDispatcher(String path) {
        try {
//            Servlet servlet = ServletDefaultHttpServlet.INSTANCE;
//            List<Filter> allNeedFilters = RecyclableUtil.newRecyclableList(0);

            Servlet servlet = servletUrlMapper.getMappingObjectByUri(path);
            List<Filter> allNeedFilters = matchFilterByPath(path);

            FilterChain filterChain = new ServletFilterChain(this,servlet, allNeedFilters);
            return ServletRequestDispatcher.newInstance(filterChain);
        } catch (Exception e) {
            logger.error("Throwing exception when getting Filter from ServletFilterRegistration of path " + path, e);
            return null;
        }
    }

    @Override
    public ServletRequestDispatcher getNamedDispatcher(String name) {
        try {
            ServletRegistration servletRegistration = null == name ? null : getServletRegistration(name);
            if (servletRegistration == null) {
                return null;
            }
            Servlet servlet = servletRegistration.getServlet();
            List<Filter> allNeedFilters = matchFilterByName(name);

            FilterChain filterChain = new ServletFilterChain(this,servlet, allNeedFilters);
            return ServletRequestDispatcher.newInstance(filterChain);
        } catch (Exception e) {
            logger.error("Throwing exception when getting Filter from ServletFilterRegistration of name " + name, e);
            return null;
        }
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        ServletRegistration registration = servletRegistrationMap.get(name);
        if(registration == null){
            return null;
        }
        return registration.getServlet();
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        List<Servlet> list = new ArrayList<>();
        for(ServletRegistration registration : servletRegistrationMap.values()){
            list.add(registration.getServlet());
        }
        return Collections.enumeration(list);
    }

    @Override
    public Enumeration<String> getServletNames() {
        List<String> list = new ArrayList<>();
        for(ServletRegistration registration : servletRegistrationMap.values()){
            list.add(registration.getName());
        }
        return Collections.enumeration(list);
    }

    @Override
    public void log(String msg) {
        logger.debug(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        logger.debug(msg,exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.debug(message,throwable);
    }

    @Override
    public String getRealPath(String path) {
        return path;
    }

    @Override
    public String getServerInfo() {
        return serverInfo;
    }

    @Override
    public String getInitParameter(String name) {
        return initParamMap.get(name);
    }

    public <T>T getInitParameter(String name,T def) {
        String value = getInitParameter(name);
        if(value == null){
            return def;
        }
        Class<?> clazz = def.getClass();
        Object valCast = TypeUtil.cast((Object) value,clazz);
        if(valCast != null && valCast.getClass().isAssignableFrom(clazz)){
            return (T) valCast;
        }
        return def;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParamMap.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParamMap.putIfAbsent(name,value) == null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributeMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributeMap.keySet());
    }

    @Override
    public void setAttribute(String name, Object object) {
        Objects.requireNonNull(name);

        if(object == null){
            removeAttribute(name);
            return;
        }

        Object oldObject = attributeMap.put(name,object);

        ServletEventListenerManager listenerManager = getServletEventListenerManager();
        if(listenerManager.hasServletContextAttributeListener()){
            listenerManager.onServletContextAttributeAdded(new ServletContextAttributeEvent(this,name,object));
            if(oldObject != null){
                listenerManager.onServletContextAttributeReplaced(new ServletContextAttributeEvent(this,name,oldObject));
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object oldObject = attributeMap.remove(name);

        ServletEventListenerManager listenerManager = getServletEventListenerManager();
        if(listenerManager.hasServletContextAttributeListener()){
            listenerManager.onServletContextAttributeRemoved(new ServletContextAttributeEvent(this,name,oldObject));
        }
    }

    @Override
    public String getServletContextName() {
        return getClass().getSimpleName();
    }

    @Override
    public ServletRegistration addServlet(String servletName, String className) {
        try {
            return addServlet(servletName, (Class<? extends Servlet>) Class.forName(className).newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServletRegistration addServlet(String servletName, Servlet servlet) {
        Servlet newServlet = servletEventListenerManager.onServletAdded(servlet);

        ServletRegistration servletRegistration = new ServletRegistration(servletName,newServlet,this);
        servletRegistrationMap.put(servletName,servletRegistration);
        return servletRegistration;
    }

    @Override
    public ServletRegistration addServlet(String servletName, Class<? extends Servlet> servletClass) {
        Servlet servlet = null;
        try {
            servlet = servletClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return addServlet(servletName,servlet);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegistrationMap.get(servletName);
    }

    @Override
    public Map<String, ServletRegistration> getServletRegistrations() {
        return servletRegistrationMap;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        try {
            return addFilter(filterName, (Class<? extends Filter>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        ServletFilterRegistration registration = new ServletFilterRegistration(filterName,filter,this);
        filterRegistrationMap.put(filterName,registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        try {
            return addFilter(filterName,filterClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return filterRegistrationMap.get(filterName);
    }

    @Override
    public Map<String, ServletFilterRegistration> getFilterRegistrations() {
        return filterRegistrationMap;
    }

    @Override
    public ServletSessionCookieConfig getSessionCookieConfig() {
        return sessionCookieConfig;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        sessionTrackingModeSet = sessionTrackingModes;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return sessionTrackingModeSet;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return sessionTrackingModeSet;
    }

    @Override
    public void addListener(String className) {
        try {
            addListener((Class<? extends EventListener>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends EventListener> void addListener(T listener) {
        Objects.requireNonNull(listener);

        ServletEventListenerManager listenerManager = getServletEventListenerManager();
        if(listener instanceof ServletContextAttributeListener){
            listenerManager.addServletContextAttributeListener((ServletContextAttributeListener) listener);

        }else if(listener instanceof ServletRequestListener){
            listenerManager.addServletRequestListener((ServletRequestListener) listener);

        }else if(listener instanceof ServletRequestAttributeListener){
            listenerManager.addServletRequestAttributeListener((ServletRequestAttributeListener) listener);

        }else if(listener instanceof HttpSessionIdListener){
            listenerManager.addHttpSessionIdListenerListener((HttpSessionIdListener) listener);

        }else if(listener instanceof HttpSessionAttributeListener){
            listenerManager.addHttpSessionAttributeListener((HttpSessionAttributeListener) listener);

        }else if(listener instanceof HttpSessionListener){
            listenerManager.addHttpSessionListener((HttpSessionListener) listener);

        }else if(listener instanceof ServletContextListener){
            listenerManager.addServletContextListener((ServletContextListener) listener);

        }else {
            throw new IllegalArgumentException("applicationContext.addListener.iae.wrongType"+
                    listener.getClass().getName());
        }
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        try {
            addListener(listenerClass.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return servletServerAddress.getHostString();
    }


}
