package com.github.netty.core;

import com.github.netty.util.ReflectUtils;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author 84215
 */
public class NettyHttpRequestWarpper implements HttpRequest{

    private final HttpRequest request;

    private List<Method> getProtocolVersionMethodList;
    private List<Method> getMethodMethodList;
    private List<Method> getUriMethodList;
    private List<Method> getDecoderResultMethodList;

    public NettyHttpRequestWarpper(HttpRequest request) {
        this.request = request;
        initMethodAdapter();
    }

    private void initMethodAdapter(){
        getProtocolVersionMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(request, "getProtocolVersion",0),
                ReflectUtils.getAccessibleMethodByName(request, "protocolVersion",0)
        );

        getMethodMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(request, "getMethod",0),
                ReflectUtils.getAccessibleMethodByName(request, "method",0)
        );

        getUriMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(request, "getUri",0),
                ReflectUtils.getAccessibleMethodByName(request, "uri",0)
        );

        getDecoderResultMethodList = Arrays.asList(
                ReflectUtils.getAccessibleMethodByName(request, "getDecoderResult",0),
                ReflectUtils.getAccessibleMethodByName(request, "decoderResult",0)
        );
    }

    public HttpMethod method() {
        return (HttpMethod) ReflectUtils.invokeMethodOnce(request,getMethodMethodList);
    }

    public String uri() {
        return (String) ReflectUtils.invokeMethodOnce(request,getUriMethodList);
    }

    public HttpVersion protocolVersion() {
        return (HttpVersion) ReflectUtils.invokeMethodOnce(request,getProtocolVersionMethodList);
    }

    public DecoderResult decoderResult() {
        return (DecoderResult) ReflectUtils.invokeMethodOnce(request,getDecoderResultMethodList);
    }

    public HttpMethod getMethod() {
        return method();
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
         request.setMethod(method);
         return this;
    }

    @Override
    public HttpRequest setUri(String uri) {
         request.setUri(uri);
         return this;
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        request.setProtocolVersion(version);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        request.setDecoderResult(result);
    }

    public String getUri() {
        return uri();
    }

    public HttpVersion getProtocolVersion() {
        return protocolVersion();
    }

    public DecoderResult getDecoderResult() {
        return decoderResult();
    }
}
