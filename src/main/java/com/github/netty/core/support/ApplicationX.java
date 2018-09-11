package com.github.netty.core.support;

import sun.misc.Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by acer01 on 2016/11/11/011.
 *
 * 轻量级容器, 支持资源注入
 *
 * 2016年11月12日 21:04:39
 */
public class ApplicationX {

    private Collection<Class<? extends Annotation>> scannerAnnotationList = new HashSet<>(
            Arrays.asList(Resource.class));

    private Collection<Class<? extends Annotation>> injectAnnotationList = new HashSet<>(
            Arrays.asList(Resource.class));

    private ClassLoader loader = getClass().getClassLoader();
    private Scanner scanner = new Scanner();
    private Injector injector = new Injector();
    private Map<Class,Object> context = new ClassInstanceMap();

    public ApplicationX() {
        addInstance(this);
//        this.loader = WebAppClassLoader.getSystemClassLoader();
//        this.loader = ClassLoader.getSystemClassLoader();
//
//        try {
//            this.loader = new WebAppClassLoader(new WebAppContext());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) {
        ApplicationX app = new ApplicationX()
                .scanner("com.github.netty")
                .inject();
    }

    public void addInjectAnnotation(Class<? extends Annotation>... classes){
        Collections.addAll(injectAnnotationList, classes);
    }

    public void addScanAnnotation(Class<? extends Annotation>... classes){
        Collections.addAll(scannerAnnotationList, classes);
    }

    public Object addInstance(Object instance){
        return addInstance(instance,true);
    }

    public Object addInstance(Object instance,boolean isInject){
        if(isInject) {
            injector.inject(instance.getClass(), instance);
        }
        return this.context.put(instance.getClass(),instance);
    }

    public ApplicationX inject(){
        try {
            for (Map.Entry<Class, Object> entry : getApplicationContext().entrySet()) {
                injector.inject(entry.getKey(),entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ApplicationX scanner(){
        try {
            for(String rootPackage : scanner.getRootPackageList()){
                scanner.doScan(rootPackage, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ApplicationX scanner(String... rootPackage){
        addScanPackage(rootPackage);
        scanner();
        return this;
    }

    public ApplicationX addExcludesPackage(String... excludesPackages){
        scanner.getExcludeList().addAll(Arrays.asList(excludesPackages));
        return this;
    }

    public ApplicationX addScanPackage(String...rootPackages){
        scanner.getRootPackageList().addAll(Arrays.asList(rootPackages));
        return this;
    }

    public <T>T get(Class<T> clazz){
        return (T) getApplicationContext().get(clazz);
    }

    public <T>T get(String clazz){
        return (T) getApplicationContext().get(clazz);
    }

    public <T>List<T> findType(Class<T> clazz){
        List<T> list = new ArrayList<T>();
        for(Map.Entry<Class,Object> in : getApplicationContext().entrySet()){
            if(clazz.isAssignableFrom(in.getKey()))
                list.add((T) in.getValue());
        }
        return list;
    }

    public <T>List<T> findImpl(Class<T> clazz){
        List<T> list = new ArrayList<T>();

        for(Map.Entry<Class,Object> in : getApplicationContext().entrySet()){
            Class key = in.getKey();
            if(clazz.isAssignableFrom(key)) {
                if(isAbstract(key))
                    continue;

                list.add((T) in.getValue());
            }
        }
        return list;
    }

    public Map<Class, Object> getApplicationContext() {
        return context;
    }

    private <T> T newInstanceByJdk(Class<T> clazz,List<?> params){
        params = params == null? Collections.emptyList() : params;
        Class[] typeArr = new Class[params.size()];

        for(int i=0; i< params.size(); i++){
            typeArr[i] = params.get(i).getClass();
        }
        try {
            Object instance = clazz.getDeclaredConstructor(typeArr).newInstance(params.toArray());
            return (T) instance;
        }catch (Exception e){
            return null;
        }
    }
    
    private <T> T newInstanceByUnsafe(Class<T> clazz){
        Object obj = null;
        try {
            obj = clazz.newInstance();
        } catch (Throwable e) {
            try {
                obj = UNSAFE.allocateInstance(clazz);
            } catch (InstantiationException e1) {
                //
            }
        }
        return (T) obj;
    }

    private static boolean isAbstract(Class clazz){
        int modifier = clazz.getModifiers();
        return Modifier.isInterface(modifier) || Modifier.isAbstract(modifier);
    }

    private static boolean isExistAnnotation(Class clazz, Collection<Class<? extends Annotation>> annotations){
        for(Annotation a : clazz.getAnnotations()){
            Class aClass = a.annotationType();
            for(Class e : annotations){
                if(e.isAssignableFrom(aClass))
                    return true;
            }
        }
        return false;
    }

    private static Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE =(Unsafe)f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return scanner.getRootPackageList() +" @ size = " + getApplicationContext().size();
    }


    /**
     * 1.扫描class文件
     * 2.创建对象并包装
     */
    private class Scanner {
        Collection<String> excludeList = new HashSet<>();
        Collection<String> rootPackageList = new ArrayList<>();

        private Collection<String> getExcludeList(){
            return this.excludeList;
        }

        private Collection<String> getRootPackageList() {
            return rootPackageList;
        }

        private Map doScan(String basePackage, Map content) throws IOException {
            String splashPath = dotToSplash(basePackage);
            URL url = loader.getResource(splashPath);
            if (url == null || existContains(url))
                return content;

            String filePath = getRootPath(url);
            List<String> names;
            if (isJarFile(filePath)) {
                names = readFromJarFile(filePath, splashPath);
            } else {
                names = readFromDirectory(filePath);
            }

            for (String name : names) {
                if (isClassFile(name)) {
                    Class clazz = toClass(name, basePackage);
                    if (clazz != null) {
                        getApplicationContext().put(clazz, newInstance(clazz));
                    }
                } else {
                    doScan(basePackage + "." + name, content);
                }
            }
            return content;
        }

        private <T>T newInstance(Class<T> clazz){
            if(isAbstract(clazz))
                return null;

            if(!isExistAnnotation(clazz, scannerAnnotationList))
                return null;

            T obj = get(clazz);
            if(obj != null)
                return obj;

            T instance = newInstanceByJdk(clazz,null);
//            if(instance == null){
//                instance = newInstanceByCglib(clazz);
//            }
            if(instance == null){
                instance = newInstanceByUnsafe(clazz);
            }
            if(instance == null) {
                throw new RuntimeException("不能创建对象");
            }
            return instance;
        }

        private boolean existContains(URL url){
            if(excludeList.isEmpty())
                return false;
            String[] urlStr = url.getPath().split("/");
            for(String s : excludeList) {
                for(String u :urlStr) {
                    if (u.equals(s))
                        return true;
                }
            }
            return false;
        }

        private Class toClass(String shortName, String basePackage) {
            StringBuilder sb = new StringBuilder();
            shortName = trimExtension(shortName);
            if(shortName.contains(basePackage))
                sb.append(shortName);
            else {
                sb.append(basePackage);
                sb.append('.');
                sb.append(shortName);
            }
            try {
                return Class.forName(sb.toString(),false,loader);
            } catch (Throwable e) {
                return null;
            }
        }

//        if(jarPath.equals("/git/api/erp.jar"))
//        jarPath = "git/api/erp.jar";
        private List<String> readFromJarFile(String jarPath, String splashedPackageName) throws IOException {
            JarInputStream jarIn = new JarInputStream(new FileInputStream(jarPath));
            JarEntry entry = jarIn.getNextJarEntry();

            List<String> nameList = new ArrayList<String>();
            while (null != entry) {
                String name = entry.getName();
                if (name.startsWith(splashedPackageName) && isClassFile(name)) {
                    nameList.add(name);
                }
                entry = jarIn.getNextJarEntry();
            }
            return nameList;
        }

        private List<String> readFromDirectory(String path) {
            File file = new File(path);
            String[] names = file.list();
            if (null == names)
                return Collections.emptyList();
            return Arrays.asList(names);
        }

        private boolean isClassFile(String name) {
            return name.endsWith(".class");
        }

        private boolean isJarFile(String name) {
            return name.endsWith(".jar");
        }

        private String getRootPath(URL url) {
            String fileUrl = url.getFile();
            int pos = fileUrl.indexOf('!');
            if (-1 == pos)
                return fileUrl;
            return fileUrl.substring(5, pos);
        }

        /**
         * "cn.fh.lightning" -> "cn/fh/lightning"
         */
        private String dotToSplash(String name) {
            return name.replaceAll("\\.", "/");
        }

        /**
         * "com/git/Apple.class" -> "com.git.Apple"
         */
        private String trimExtension(String name) {
            int pos = name.indexOf('.');
            if (-1 != pos)
                name = name.substring(0, pos);
            return name.replace("/",".");
        }

        /**
         * /application/home -> /home
         */
        private  String trimURI(String uri) {
            String trimmed = uri.substring(1);
            int splashIndex = trimmed.indexOf('/');
            return trimmed.substring(splashIndex);
        }
    }

    /**
     * 自动注入
     */
    private class Injector {

//        private Object createRpcResource(RpcResource a,Field field,Object obj) throws MalformedURLException {
//            return create0(field.getType(),obj);
//
//            RpcServer rpcServer = get(RpcServer.class);
//            String servicePath = a.value();
//
//            ServiceNode serviceNode = RpcServer.getServiceNode(servicePath);
//            if(serviceNode == null)
//                return null;
//
//            boolean isOnlineServer = Tools.isOnlineServer();
//            String domain;
//            int port;
//            if(isOnlineServer){
//                port = serviceNode.getPort();
//                domain = serviceNode.getDomain();
//            }else {
//                port = serviceNode.getTestPort();
//                domain = serviceNode.getTestDomain();
//            }
//            String url = domain + ":" + port + servicePath;
//            Object obj = rpcServer.getRpcFactory().createApi(field.getType(),url);
//            return obj;
//        }

        private Class findType(Annotation resourceAnn, Field field){
            if(resourceAnn == null){
                return field.getType();
            }

            Class resourceType = field.getType();
            Class resourceAnnType = Object.class;
            if(resourceAnn instanceof Resource) {
                resourceAnnType = ((Resource) resourceAnn).type();
            }else if(resourceAnn instanceof javax.annotation.Resource){
                resourceAnnType = ((javax.annotation.Resource) resourceAnn).type();
            }

            Class type;
            if(resourceAnnType != Object.class && resourceType.isAssignableFrom(resourceAnnType)){
                type = resourceAnnType;
            }else {
                type = resourceType;
            }
            return type;
        }

        private Annotation findAnnotation(Annotation[] annotations){
            for(Annotation annotation : annotations){
                Class<? extends Annotation> annotationClass = annotation.getClass();
                for(Class<? extends Annotation> injectAnnotationClass : injectAnnotationList) {
                    if (injectAnnotationClass == annotationClass){
                        return annotation;
                    }
                }
            }
            return null;
        }

        private Object findResource(Field field, Object target){
            Annotation annotation = findAnnotation(field.getDeclaredAnnotations());
            Class type = findType(annotation,field);
            if(!isAbstract(type)) {
                return get(type);
            }

            List implList = findImpl(type);
            for(Object impl : implList){
                //防止 自身要注入自身 已经实现的接口 从而发生死循环调用
                if(impl != target)
                    return impl;
            }
            return null;

//            RpcResource rpcResource = field.getAnnotation(RpcResource.class);
//            if(rpcResource != null){
//                return createRpcResource(rpcResource,field,obj);
//            }
        }

        private void inject(Class clazz, Object target) {
            for(Class cClazz = clazz; cClazz!=Object.class; cClazz = cClazz.getSuperclass()) {
                for (Field field : cClazz.getDeclaredFields()) {
                    if(Modifier.isFinal(field.getModifiers())){
                        continue;
                    }

                    Object resource = findResource(field, target);
                    if (null == resource)
                        continue;

                    try {
                        boolean isAccessible = field.isAccessible();
                        try {
                            field.setAccessible(true);

                            Object oldValue = field.get(target);
                            if(oldValue != null){
                                continue;
                            }

                            field.set(target, resource);
                        } finally {
                            field.setAccessible(isAccessible);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 存储class实例
     */
    public class ClassInstanceMap extends ConcurrentHashMap<Class,Object> {
        private ConcurrentHashMap<String, Object> strClassMap = new ConcurrentHashMap<>();
        private ClassInstanceMap(){}

        @Override
        public Object put(Class key, Object value) {
            if(value == null)
                return null;

            String annotationKey = getAnnotationValue(key.getAnnotations());
            if(!annotationKey.isEmpty())
                strClassMap.put(annotationKey,value);

            strClassMap.put(key.getName(), value);
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends Class, ?> m) {
            for(Entry<? extends Class, ?> e : m.entrySet()){
                put(e.getKey(),e.getValue());
            }
        }

        @Override
        public Object get(Object key) {
            if (key instanceof String)
                return strClassMap.get(key);
            return super.get(key);
        }

        @Override
        public Object remove(Object key) {

            if (key instanceof String) {
                return strClassMap.remove(key);
            }

            if(key instanceof Class) {
                Class classKey = (Class) key;
                String annKey = getAnnotationValue(classKey.getAnnotations());
                if(!annKey.isEmpty()) {
                    strClassMap.remove(annKey);
                }
                strClassMap.remove(classKey.getName());
                Object removeValue = super.remove(classKey);
                return removeValue;
            }

            return null;
        }

        private String getAnnotationValue(Annotation[] annotations){
            for(Annotation a : annotations) {
                Class ann = a.getClass();
                if (Resource.class.isAssignableFrom(ann)) {
                    return ((Resource)a).value();
                }
            }
            return "";
        }
    }


    @Target({TYPE, FIELD})
    @Retention(RUNTIME)
    public @interface Resource {
        String value() default "";
        Class<?> type() default java.lang.Object.class;
        int sort() default 100;
    }

}
