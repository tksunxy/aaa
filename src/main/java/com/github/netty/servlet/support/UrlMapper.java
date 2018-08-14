package com.github.netty.servlet.support;

import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.Recyclable;
import com.github.netty.core.util.RecyclableUtil;
import com.github.netty.core.util.TodoOptimize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 映射规范
 * 在web应用部署描述符中，以下语法用于定义映射：
 * ■  以‘/’字符开始、以‘/*’后缀结尾的字符串用于路径匹配。
 * ■  以‘*.’开始的字符串用于扩展名映射。
 * ■  空字符串“”是一个特殊的URL模式，其精确映射到应用的上下文根，即，http://host:port/context-root/
 * 请求形式。在这种情况下，路径信息是‘/’且servlet路径和上下文路径是空字符串（“”）。
 * ■  只包含“/”字符的字符串表示应用的“default”servlet。在这种情况下，servlet路径是请求URL减去上
 * 下文路径且路径信息是null。
 * ■  所以其他字符串仅用于精确匹配。
 * 如果一个有效的web.xml（在从fragment 和注解合并了信息后）包含人任意的url-pattern，其映射到多个servlet，那么部署将失败。
 *
 *
 * 示例映射集合
 * 请看下面的一组映射：
 *  表12-1  示例映射集合
 *      Path Pattern            Servlet
 *
 *      /foo/bar/*              servlet1
 *      /baz/*                  servlet2
 *      /catalog                servlet3
 *      *.bop                   servlet4
 * 将产生以下行为：
 *  表12-2   传入路径应用于示例映射
 *      Incoming Path           Servlet Handling Request
 *
 *      /foo/bar/index.html     servlet1
 *      /foo/bar/index.bop      servlet1
 *      /baz                    servlet2
 *      /baz/index.html         servlet2
 *      /catalog                servlet3
 *      /catalog/index.html    “default”  servlet
 *      /catalog/racecar.bop    servlet4
 *      /index.bop              servlet4
 * 请注意，在/catalog/index.html和/catalog/racecar.bop的情况下，不使用映射到“/catalog”的servlet，因为不是精确匹配的
 *
 * @author acer01
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
                    if(singlePattern) {
                        throw new IllegalArgumentException("URL Pattern('" + urlPattern + "') already exists!");
                    }
                }
            }
            element = new Element(pattern, object, objectName);
            urlPatternContext.wildcardObjectList.add(element);
            urlPatternContext.wildcardObjectList.sort((o1, o2) -> o2.pattern.compareTo(o1.pattern));
            urlPatternContext.addElement(element);
//            log.debug("Curretn Wildcard URL Pattern List = " + Arrays.toString(urlPatternContext.wildcardObjectList.toArray()));
            return;
        }

        // 扩展名匹配
        if (urlPattern.startsWith("*.")) {
            String pattern = urlPattern.substring(2);
            if (urlPatternContext.extensionObjectMap.get(pattern) != null) {
                if(singlePattern) {
                    throw new IllegalArgumentException("URL Pattern('" + urlPattern + "') already exists!");
                }
            }
            element = new Element(pattern, object, objectName);
            urlPatternContext.extensionObjectMap.put(pattern, element);
            urlPatternContext.addElement(element);
//            log.debug("Curretn Extension URL Pattern List = " + Arrays.toString(urlPatternContext.extensionObjectMap.keySet().toArray()));
            return;
        }

        // Default资源匹配
        if (urlPattern.length() ==1 && urlPattern.charAt(0) == '/') {
            if (urlPatternContext.defaultObject != null) {
                if(singlePattern) {
                    throw new IllegalArgumentException("URL Pattern('" + urlPattern + "') already exists!");
                }
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
            if(singlePattern) {
                throw new IllegalArgumentException("URL Pattern('" + urlPattern + "') already exists!");
            }
        }
        element = new Element(pattern, object, objectName);
        urlPatternContext.exactObjectMap.put(pattern, element);
        urlPatternContext.addElement(element);
//        log.debug("Curretn Exact URL Pattern List = " + Arrays.toString(urlPatternContext.exactObjectMap.keySet().toArray()));
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
        if (urlPattern.length() == 2 && urlPattern.charAt(0)=='*' && urlPattern.charAt(1)=='.') {
            String pattern = urlPattern.substring(2);
            urlPatternContext.extensionObjectMap.remove(pattern);
            return;
        }

        // Default资源匹配
        if ("/".equals(urlPattern)) {
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

    public T getMappingObjectByUri(String absoluteUri) {
        MappingData mappingData = getMapping(absoluteUri);

        if(mappingData == null){
            return null;
        }

        T result = (T) mappingData.object;
        mappingData.recycle();
        return result;
    }

    public List<T> getMappingObjectsByUri(String absoluteUri) {
        List<T> list = RecyclableUtil.newRecyclableList(10);

        for(Element element : urlPatternContext.totalObjectList){
            if("*".equals(element.pattern) || "/".equals(element.pattern) || "/*".equals(element.pattern) || "/**".equals(element.pattern)){
                list.add(element.object);
            }else if(absoluteUri.startsWith(element.pattern)){
                list.add(element.object);
            }
        }
        return list;
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

    @TodoOptimize("1.暂不考虑JSP的处理 ,2.暂不考虑Welcome资源 ,3.暂不考虑请求静态目录资源")
    private MappingData getMapping(String absolutePath) {
        String path = toPath(absolutePath);
        // 路径为空时，重定向到“/”
        if(path == null || "/".equals(path)){
            if (urlPatternContext.defaultObject == null) {
                return null;
            }
            return MappingData.newInstance (urlPatternContext.defaultObject.object,urlPatternContext.defaultObject.objectName);
        }

        //1. 暂不考虑JSP的处理

        // 优先进行精确匹配
        Element element = urlPatternContext.exactObjectMap.get(path);
        if (element != null) {
            return MappingData.newInstance(element.object,element.objectName);
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
            return MappingData.newInstance(element.object,element.objectName);
        }


        // 后缀名匹配
        int dotInx = path.lastIndexOf('.');
        path = path.substring(dotInx + 1);
        element = urlPatternContext.extensionObjectMap.get(path);
        if (element != null) {
            return MappingData.newInstance(element.object,element.objectName);
        }


        //2. 暂不考虑Welcome资源

        // Default Servlet
        if (urlPatternContext.defaultObject != null) {
            return MappingData.newInstance(urlPatternContext.defaultObject.object,urlPatternContext.defaultObject.objectName);
        }

        //3. 暂不考虑请求静态目录资源
        if (path.charAt(path.length() - 1) != '/') {

        }
        return null;
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

    public static class MappingData<T> implements Recyclable{
        T object = null;
        String objectName;
        String redirectPath ;

        private static final AbstractRecycler<MappingData> RECYCLER = new AbstractRecycler<MappingData>() {
            @Override
            protected MappingData newInstance() {
                return new MappingData();
            }
        };

        public static MappingData newInstance(Object object, String objectName) {
            MappingData instance = RECYCLER.get();

            instance.object = object;
            instance.objectName = objectName;
            return instance;
        }

        @Override
        public void recycle() {
            object = null;
            objectName = null;
            redirectPath = null;
            RECYCLER.recycle(MappingData.this);
        }
    }

}