package com.github.netty.servlet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import java.util.*;

/**
 * @author acer01
 *  2018/7/14/014
 */
public class ServletFilterRegistration implements FilterRegistration,FilterRegistration.Dynamic {

    private String filterName;
    private Filter filter;
    private FilterConfig filterConfig;
    private ServletContext servletContext;
    private Map<String,String> initParameterMap;
    private ServletFilterRegistration self;
    private Set<String> mappingSet;
    private Set<String> servletNameMappingSet;
    private boolean asyncSupported;

    public ServletFilterRegistration(String filterName, Filter servlet,ServletContext servletContext) {
        this.filterName = filterName;
        this.filter = servlet;
        this.servletContext = servletContext;
        this.initParameterMap = new HashMap<>();
        this.mappingSet = new HashSet<>();
        this.servletNameMappingSet = new HashSet<>();
        this.asyncSupported = false;
        this.self = this;

        this.filterConfig = new FilterConfig(){

            @Override
            public String getFilterName() {
                return self.filterName;
            }

            @Override
            public ServletContext getServletContext() {
                return self.servletContext;
            }

            @Override
            public String getInitParameter(String name) {
                return self.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(self.getInitParameters().keySet());
            }
        };
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    public Filter getFilter() {
        return filter;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    @Override
    public String getName() {
        return filterName;
    }

    @Override
    public String getClassName() {
        return filter.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameterMap.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        this.initParameterMap = initParameters;
        return initParameterMap.keySet();
    }

    @Override
    public Map<String, String> getInitParameters() {
        return initParameterMap;
    }

    //==============

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.asyncSupported = isAsyncSupported;
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
        servletNameMappingSet.addAll(Arrays.asList(servletNames));
//        for(String servletName : servletNames) {
//            servletContext.find
//            servletContext.addFilterMapping(servletName,filterName,filter);
//        }
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return servletNameMappingSet;
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
        mappingSet.addAll(Arrays.asList(urlPatterns));
        for(String pattern : urlPatterns) {
            servletContext.addFilterMapping(pattern,filterName,filter);
        }
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return mappingSet;
    }

}
