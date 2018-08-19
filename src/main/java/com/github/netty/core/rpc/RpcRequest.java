package com.github.netty.core.rpc;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by acer01 on 2018/8/19/019.
 */
public class RpcRequest implements Serializable {

    //请求ID
    private Integer requestId;
    //类名称
    private String serviceName;
    //方法名称
    private String methodName;
    //参数值
    private Object[] parametersVal;

    public RpcRequest() {}

    public RpcRequest(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParametersVal() {
        return parametersVal;
    }

    public void setParametersVal(Object[] parametersVal) {
        this.parametersVal = parametersVal;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId=" + requestId +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parametersVal=" + Arrays.toString(parametersVal) +
                '}';
    }
}
