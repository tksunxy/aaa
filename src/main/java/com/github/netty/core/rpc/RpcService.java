package com.github.netty.core.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

/**
 * Created by acer01 on 2018/8/19/019.
 */
public class RpcService {

    private String serviceId;
    private Object service;
    private List<Method> methodList;
    private List<Class> interfaceList;

    public RpcService(String serviceId, Object service) {
        this.serviceId = serviceId;
        this.service = service;

        this.interfaceList = getInterfaceList(service);
        if(interfaceList.isEmpty()){
            throw new RuntimeException("rpc服务必须至少拥有一个接口");
        }

        this.methodList = getMethodList(interfaceList);
        if(methodList.isEmpty()){
            throw new RuntimeException("rpc服务接口必须至少拥有一个方法");
        }
    }

    public Object invoke(String methodName,Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = getMethod(methodName);
        if(method == null){
            throw new NoSuchMethodException("not found method ["+methodName+"]");
        }
        Object result = method.invoke(service,args);
        return result;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Method getMethod(String methodName) {
        if(methodList instanceof RandomAccess) {
            int size = methodList.size();
            for (int i=0; i<size; i++){
                Method method = methodList.get(i);
                if(method.getName().equals(methodName)){
                    return method;
                }
            }
        }else {
            for(Method method : methodList){
                if(method.getName().equals(methodName)){
                    return method;
                }
            }
        }
        return null;
    }

    public Object getService() {
        return service;
    }


    private List<Method> getMethodList(List<Class> interfaceList){
        List<Method> methodList = new ArrayList<>();
        for(Class interfaceClazz : interfaceList) {
            methodList.addAll(Arrays.asList(interfaceClazz.getMethods()));
        }
        return methodList;
    }

    private List<Class> getInterfaceList(Object source){
        List<Class> interfaceList = new ArrayList<>();
        Class sourceClass = source.getClass();
        for(Class currClass = sourceClass; currClass != null; currClass = currClass.getSuperclass()){
            interfaceList.addAll(Arrays.asList(currClass.getInterfaces()));
        }
        return interfaceList;
    }

}