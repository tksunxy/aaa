package com.github.netty.core.adapter;

import com.github.netty.util.ReflectUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用于兼容 netty4 与netty5
 * @author acer01
 * 2018/7/28/028
 */
public class NettyHttpRequest implements HttpRequest,Wrapper<HttpRequest>{

    private HttpRequest source;
    private Class sourceClass;
    private final Object lock = new Object();
    private Channel channel;

    private List<Method> getProtocolVersionMethodList;
    private List<Method> getMethodMethodList;
    private List<Method> getUriMethodList;
    private List<Method> getDecoderResultMethodList;
    
    public NettyHttpRequest(HttpRequest source,Channel channel) {
        wrap(source);
        this.channel = Objects.requireNonNull(channel);
    }

    public Channel getChannel() {
        return channel;
    }

    public HttpMethod getMethod() {
        if(getMethodMethodList == null){
            synchronized (lock) {
                if(getMethodMethodList == null) {
                    getMethodMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "getMethod",0),
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "method",0)
                    );
                }
            }
        }
        return (HttpMethod) ReflectUtil.invokeMethodOnce(source,getMethodMethodList);
    }

    public String getUri() {
        if(getUriMethodList == null){
            synchronized (lock) {
                if(getUriMethodList == null) {
                    getUriMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "getUri",0),
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "uri",0)
                    );
                }
            }
        }
        return (String) ReflectUtil.invokeMethodOnce(source,getUriMethodList);
    }

    public HttpVersion getProtocolVersion() {
        if(getProtocolVersionMethodList == null){
            synchronized (lock) {
                if(getProtocolVersionMethodList == null) {
                    getProtocolVersionMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "getProtocolVersion",0),
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "protocolVersion",0)
                    );
                }
            }
        }
        return (HttpVersion) ReflectUtil.invokeMethodOnce(source,getProtocolVersionMethodList);
    }

    public DecoderResult getDecoderResult() {
        if(getDecoderResultMethodList == null){
            synchronized (lock) {
                if(getDecoderResultMethodList == null) {
                    getDecoderResultMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "getDecoderResult",0),
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "decoderResult",0)
                    );
                }
            }
        }
        return (DecoderResult) ReflectUtil.invokeMethodOnce(source,getDecoderResultMethodList);
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
         source.setMethod(method);
         return this;
    }

    @Override
    public HttpRequest setUri(String uri) {
         source.setUri(uri);
         return this;
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        source.setProtocolVersion(version);
        return this;
    }

    public HttpMethod method() {
        return getMethod();
    }

    public String uri() {
        return getUri();
    }

    public HttpVersion protocolVersion() {
        return getProtocolVersion();
    }

    public DecoderResult decoderResult() {
        return getDecoderResult();
    }

    @Override
    public HttpHeaders headers() {
        return source.headers();
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        source.setDecoderResult(result);
    }

    @Override
    public void wrap(HttpRequest source) {
        Objects.requireNonNull(source);
        this.source = source;
        this.sourceClass = source.getClass();
    }

    @Override
    public HttpRequest unwrap() {
        return source;
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return source.equals(obj);
    }


    @Override
    public String toString() {
        return "NettyHttpRequest{" +
                "sourceClass=" + sourceClass +
                '}';
    }
}
