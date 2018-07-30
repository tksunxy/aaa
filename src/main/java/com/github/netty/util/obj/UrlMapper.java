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

    public UrlMapper() {
        this("");
    }

    public UrlMapper(String contextPath) {
        this.urlPatternContext = new UrlPatternContext();
        this.contextPath = contextPath;
    }

    /**
     * 增加映射关系
     *
     * @param urlPattern  urlPattern
     * @param object     对象
     * @param objectName 对象名称
     * @throws RuntimeException 异常
     */
    public void addMapping(String urlPattern, T object, String objectName) throws RuntimeException {
        Objects.requireNonNull(urlPattern);
        Objects.requireNonNull(object);
        Objects.requireNonNull(objectName);

        // 路径匹配
        if (urlPattern.endsWith("/*")) {
            String pattern = urlPattern.substring(0, urlPattern.length() - 1);
            for (MapElement ms : urlPatternContext.wildcardObjectList) {
                if (ms.pattern.equals(pattern)) {
                    throw new RuntimeException("URL Pattern('" + urlPattern + "') already exists!");
                }
            }
            MapElement newServlet = new MapElement(pattern, object, objectName);
            urlPatternContext.wildcardObjectList.add(newServlet);
            urlPatternContext.wildcardObjectList.sort((o1, o2) -> o2.pattern.compareTo(o1.pattern));
            log.debug("Curretn Wildcard URL Pattern List = " + Arrays.toString(urlPatternContext.wildcardObjectList.toArray()));
            return;
        }

        // 扩展名匹配
        if (urlPattern.startsWith("*.")) {
            String pattern = urlPattern.substring(2);
            if (urlPatternContext.extensionObjectMap.get(pattern) != null) {
                throw new RuntimeException("URL Pattern('" + urlPattern + "') already exists!");
            }
            MapElement newServlet = new MapElement(pattern, object, objectName);
            urlPatternContext.extensionObjectMap.put(pattern, newServlet);
            log.debug("Curretn Extension URL Pattern List = " + Arrays.toString(urlPatternContext.extensionObjectMap.keySet().toArray()));
            return;
        }

        // Default资源匹配
        if (urlPattern.equals("/")) {
            if (urlPatternContext.defaultObject != null) {
                throw new RuntimeException("URL Pattern('" + urlPattern + "') already exists!");
            }
            urlPatternContext.defaultObject = new MapElement("", object, objectName);
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
            throw new RuntimeException("URL Pattern('" + urlPattern + "') already exists!");
        }
        MapElement newServlet = new MapElement(pattern, object, objectName);
        urlPatternContext.exactObjectMap.put(pattern, newServlet);
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

    public String getMappingObjectName(String absoluteUri) {
        MappingData mappingData = getMapping(absoluteUri);
        return mappingData == null? null : mappingData.objectName;
    }

    private MappingData getMapping(String absolutePath) {
        MapElement mapElement = null;

        // 处理ContextPath，获取访问的相对URI
        boolean noServletPath = absolutePath.equals(contextPath) || absolutePath.equals(contextPath + "/");
        if (!absolutePath.startsWith(contextPath)) {
            return null;
        }

        String path = noServletPath ? "/" : absolutePath.substring(contextPath.length());
        //去掉查询字符串
        int queryInx = path.indexOf('?');
        if(queryInx > -1){
            path = path.substring(0, queryInx);
        }

        // 优先进行精确匹配
        mapElement = urlPatternContext.exactObjectMap.get(path);
        if (mapElement != null) {
            return new MappingData(mapElement.object,mapElement.objectName);
        }

        // 然后进行路径匹配
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        for (MapElement ms : urlPatternContext.wildcardObjectList) {
            if (path.startsWith(ms.pattern)) {
                mapElement = ms;
                break;
            }
        }
        if (mapElement != null) {
            return new MappingData(mapElement.object,mapElement.objectName);
        }

        //TODO 暂不考虑JSP的处理

        // 路径为空时，重定向到“/”
        if (noServletPath && urlPatternContext.defaultObject != null) {
            return new MappingData(urlPatternContext.defaultObject.object,urlPatternContext.defaultObject.objectName);
        }

        // 后缀名匹配
        int dotInx = path.lastIndexOf('.');
        path = path.substring(dotInx + 1);
        mapElement = urlPatternContext.extensionObjectMap.get(path);
        if (mapElement != null) {
            return new MappingData(mapElement.object,mapElement.objectName);
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

    public static void main(String[] args) {
        UrlMapper urlMapper = new UrlMapper<>("/hh");
        urlMapper.addMapping("/sys/add","1","add");
        urlMapper.addMapping("/db/get","2","get");

        String name = urlMapper.getMappingObjectName("/hh/sys/add");
        System.out.println();
    }


    private class UrlPatternContext {
        //默认Servlet
        MapElement defaultObject = null;
        //精确匹配
        Map<String, MapElement> exactObjectMap = new HashMap<>();
        //路径匹配
        List<MapElement> wildcardObjectList = new LinkedList<>();
        //扩展名匹配
        Map<String, MapElement> extensionObjectMap = new HashMap<>();
    }

    private class MapElement {
        final String pattern;
        final T object;
        final String objectName;
        
        MapElement(String pattern,T object, String objectName) {
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