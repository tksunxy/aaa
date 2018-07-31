package com.github.netty.core;

import com.github.netty.util.ReflectUtils;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author acer01
 * 2018/7/28/028
 */
public class NettyHttpResponse extends DefaultHttpResponse {

    private List<Method> getStatusMethodList;
    private List<Method> setStatusMethodList;
    private List<Method> getProtocolVersionMethodList;

    public NettyHttpResponse(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
        initMethodAdapter();
    }

    public NettyHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders) {
        super(version, status, validateHeaders);
        initMethodAdapter();
    }

    private void initMethodAdapter(){
        getStatusMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(this, "getStatus",0),
                ReflectUtils.getAccessibleMethodByName(this, "status",0)
        );

        setStatusMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(this, "status",1),
                ReflectUtils.getAccessibleMethodByName(this, "setStatus",1)
        );

        getProtocolVersionMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(this, "getProtocolVersion",0),
                ReflectUtils.getAccessibleMethodByName(this, "protocolVersion",0)
        );
    }

    public HttpResponseStatus getHttpStatus() {
        return (HttpResponseStatus) ReflectUtils.invokeMethodOnce(this,getStatusMethodList);
    }

    public void setHttpStatus(HttpResponseStatus status) {
        ReflectUtils.invokeMethodOnce(this,setStatusMethodList,status);
    }

}
