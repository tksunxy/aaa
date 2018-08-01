package com.github.netty.util.obj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 保存，计算URL-pattern与请求路径的匹配关系
 *
 * @author Leibniz.Hu
 * Created on 2017-08-25 11:32.
 */
public class UrlMapper<T> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private UrlPatternContext urlPatternContext;
    private String contextPath;

    private final boolean singlePattern;

    public UrlMapper(String contextPath,boolean singlePattern) {
        this.urlPatternContext = new UrlPatternContext();
        this.contextPath = contextPath;
        this.singlePattern = singlePattern;
    }

    /**
     * 增加映射关系
     *
     * @param urlPattern  urlPattern
     * @param object     对象
     * @param objectName 对象名称
     * @throws IllegalArgumentException 异常
     */
    public void addMapping(String urlPattern, T object, String objectName) throws IllegalArgumentException {
        Objects.requireNonNull(urlPattern);
        Objects.requireNonNull(object);
        Objects.requireNonNull(objectName);

        Element element;

        // 路径匹配
        if (urlPattern.endsWith("/*")) {
            String pattern = urlPattern.substring(0, urlPattern.length() - 1);
            for (Element ms : urlPatternContext.wildcardObjectList) {
                if (ms.pattern.equals(pattern)) {
                    throwsIf("URL Pattern('" + urlPattern + "') already exists!",singlePattern);
                }
            }
            element = new Element(pattern, object, objectName);
            urlPatternContext.wildcardObjectList.add(element);
            urlPatternContext.wildcardObjectList.sort((o1, o2) -> o2.pattern.compareTo(o1.pattern));
            urlPatternContext.addElement(element);
            log.debug("Curretn Wildcard URL Pattern List = " + Arrays.toString(urlPatternContext.wildcardObjectList.toArray()));
            return;
        }

        // 扩展名匹配
        if (urlPattern.startsWith("*.")) {
            String pattern = urlPattern.substring(2);
            if (urlPatternContext.extensionObjectMap.get(pattern) != null) {
                throwsIf("URL Pattern('" + urlPattern + "') already exists!",singlePattern);
            }
            element = new Element(pattern, object, objectName);
            urlPatternContext.extensionObjectMap.put(pattern, element);
            urlPatternContext.addElement(element);
            log.debug("Curretn Extension URL Pattern List = " + Arrays.toString(urlPatternContext.extensionObjectMap.keySet().toArray()));
            return;
        }

        // Default资源匹配
        if (urlPattern.equals("/")) {
            if (urlPatternContext.defaultObject != null) {
                throwsIf("URL Pattern('" + urlPattern + "') already exists!",singlePattern);
            }
            element = new Element("", object, objectName);
            urlPatternContext.defaultObject = element;
            urlPatternContext.addElement(element);
            return;
        }

        // 精确匹配
        String pattern;
        if (urlPattern.length() == 0) {
            pattern = "/";
        } else {
            pattern = urlPattern;
        }
        if (urlPatternContext.exactObjectMap.get(pattern) != null) {
            throwsIf("URL Pattern('" + urlPattern + "') already exists!",singlePattern);
        }
        element = new Element(pattern, object, objectName);
        urlPatternContext.exactObjectMap.put(pattern, element);
        urlPatternContext.addElement(element);
        log.debug("Curretn Exact URL Pattern List = " + Arrays.toString(urlPatternContext.exactObjectMap.keySet().toArray()));
    }

    /**
     * 删除映射关系
     *
     * @param urlPattern url匹配规则
     */
    public void removeMapping(String urlPattern) {
        //路径匹配
        if (urlPattern.endsWith("/*")) {
            String pattern = urlPattern.substring(0, urlPattern.length() - 2);
            urlPatternContext.wildcardObjectList.removeIf(mappedServlet -> mappedServlet.pattern.equals(pattern));
            return;
        }

        // 扩展名匹配
        if (urlPattern.startsWith("*.")) {
            String pattern = urlPattern.substring(2);
            urlPatternContext.extensionObjectMap.remove(pattern);
            return;
        }

        // Default资源匹配
        if (urlPattern.equals("/")) {
            urlPatternContext.defaultObject = null;
            return;
        }

        // 精确匹配
        String pattern;
        if (urlPattern.length() == 0) {
            pattern = "/";
        } else {
            pattern = urlPattern;
        }
        urlPatternContext.exactObjectMap.remove(pattern);
    }

    public String getMappingObjectNameByUri(String absoluteUri) {
        MappingData mappingData = getMapping(absoluteUri);
        return mappingData == null? null : mappingData.objectName;
    }

    public T getMappingObjectByUri(String absoluteUri) {
        MappingData mappingData = getMapping(absoluteUri);
        return mappingData == null? null : mappingData.object;
    }

    public List<T> getMappingObjectsByUri(String absoluteUri) {
        List<T> list = new LinkedList<>();

        for(Element element : urlPatternContext.totalObjectList){
            if("*".equals(element.pattern) || "/".equals(element.pattern) || "/*".equals(element.pattern) || "/**".equals(element.pattern)){
                list.add(element.object);
            }else if(absoluteUri.startsWith(element.pattern)){
                list.add(element.object);
            }

        }
        return null;
    }

    private String toPath(String absolutePath){
        // 处理ContextPath，获取访问的相对URI
        boolean noContextPath = absolutePath.equals(contextPath) || absolutePath.equals(contextPath + "/");
        if (!absolutePath.startsWith(contextPath)) {
            return null;
        }

        String path = noContextPath ? "/" : absolutePath.substring(contextPath.length());
        //去掉查询字符串
        int queryInx = path.indexOf('?');
        if(queryInx > -1){
            path = path.substring(0, queryInx);
        }
        return path;
    }

    private MappingData getMapping(String absolutePath) {
        String path = toPath(absolutePath);
        // 路径为空时，重定向到“/”
        if(path == null || "/".equals(path)){
            if (urlPatternContext.defaultObject == null) {
                return null;
            }
            return new MappingData(urlPatternContext.defaultObject.object,urlPatternContext.defaultObject.objectName);
        }

        //TODO 暂不考虑JSP的处理

        // 优先进行精确匹配
        Element element = urlPatternContext.exactObjectMap.get(path);
        if (element != null) {
            return new MappingData(element.object,element.objectName);
        }

        // 然后进行路径匹配
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        for (Element ms : urlPatternContext.wildcardObjectList) {
            if (path.startsWith(ms.pattern)) {
                element = ms;
                break;
            }
        }
        if (element != null) {
            return new MappingData(element.object,element.objectName);
        }


        // 后缀名匹配
        int dotInx = path.lastIndexOf('.');
        path = path.substring(dotInx + 1);
        element = urlPatternContext.extensionObjectMap.get(path);
        if (element != null) {
            return new MappingData(element.object,element.objectName);
        }


        //TODO 暂不考虑Welcome资源

        // Default Servlet
        if (urlPatternContext.defaultObject != null) {
            return new MappingData(urlPatternContext.defaultObject.object,urlPatternContext.defaultObject.objectName);
        }

        //TODO 暂不考虑请求静态目录资源
        if (path.charAt(path.length() - 1) != '/') {

        }
        return null;
    }

    private void throwsIf(String msg,boolean isThrows) throws IllegalArgumentException{
        if(isThrows) {
            throw new IllegalArgumentException(msg);
        }
    }

    private class UrlPatternContext {
        //默认Servlet
        Element defaultObject = null;
        //精确匹配
        Map<String, Element> exactObjectMap = new HashMap<>();
        //路径匹配
        List<Element> wildcardObjectList = new LinkedList<>();
        //扩展名匹配
        Map<String, Element> extensionObjectMap = new HashMap<>();
        //全部对象
        List<Element> totalObjectList = new LinkedList<>();

        public void addElement(Element mapElement){
            totalObjectList.add(mapElement);
        }
    }

    private class Element {
        final String pattern;
        final T object;
        final String objectName;
        
        Element(String pattern, T object, String objectName) {
            this.pattern = pattern;
            this.object = object;
            this.objectName = objectName;
        }
    }

    public class MappingData {
        T object = null;
        String objectName;
        String redirectPath ;

        public MappingData(T object, String objectName) {
            this.object = object;
            this.objectName = objectName;
        }

        public void recycle() {
            object = null;
            objectName = null;
            redirectPath = null;
        }
    }

}