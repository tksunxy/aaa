package com.github.netty.core.rpc;

import com.github.netty.core.rpc.codec.RpcDataCodec;

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
    private RpcServer rpcServer;

    public RpcService(String serviceId, Object service,RpcServer rpcServer) {
        this.serviceId = serviceId;
        this.service = service;
        this.rpcServer = rpcServer;

        List<Class> interfaceList = getInterfaceList(service);
        if(interfaceList.isEmpty()){
            throw new RuntimeException("rpc服务必须至少拥有一个接口");
        }

        this.methodList = getMethodList(interfaceList);
        if(methodList.isEmpty()){
            throw new RuntimeException("rpc服务接口必须至少拥有一个方法");
        }
    }

    public Object invoke(String methodName,Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int argsCount = args == null?0: args.length;
        Method method = getMethod(methodName,argsCount);
        if(method == null){
            throw new NoSuchMethodException("not found method ["+methodName+"]");
        }

        if(argsCount > 0) {
            checkTypeAutoCast(method.getParameterTypes(),args);
        }

        Object result = method.invoke(service,args);
        return result;
    }

    /**
     * 检查参数类型并自动转换
     * @param types 类型
     * @param args 参数
     */
    private void checkTypeAutoCast(Class<?>[] types,Object[] args){
        RpcDataCodec rpcDataCodec = rpcServer.getRpcDataCodec();
        int size = types.length;
        for (int i = 0; i < size; i++) {
            Object arg = args[i];
            Class type = types[i];

            //type 所对应类信息是arg对象所对应的类信息的父类或者是父接口，简单理解即type是arg的父类或接口
            if(!type.isAssignableFrom(arg.getClass())){
                args[i] = rpcDataCodec.cast(arg, type);
            }
        }
    }

    public String getServiceId() {
        return serviceId;
    }

    public Method getMethod(String methodName,int argsCount) {
        if(methodList instanceof RandomAccess) {
            int size = methodList.size();
            for (int i=0; i<size; i++){
                Method method = methodList.get(i);
                if(method.getName().equals(methodName) && method.getParameterCount() == argsCount){
                    return method;
                }
            }
        }else {
            for(Method method : methodList){
                if(method.getName().equals(methodName) && method.getParameterCount() == argsCount){
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