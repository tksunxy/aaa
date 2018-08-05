package com.github.netty.core.adapter;

import com.github.netty.core.constants.VersionConstants;
import com.github.netty.util.ReflectUtil;
import io.netty.buffer.ByteBuf;
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
public class NettyHttpRequest implements FullHttpRequest,Wrapper<FullHttpRequest>{

    private FullHttpRequest source;
    private Class sourceClass;
    private final Object lock = new Object();
    private Channel channel;

    private List<Method> getProtocolVersionMethodList;
    private List<Method> getMethodMethodList;
    private List<Method> getUriMethodList;
    private List<Method> getDecoderResultMethodList;
    private List<Method> touchMethodList;
    private List<Method> touch1MethodList;
    private List<Method> copyMethodList;

    public NettyHttpRequest(FullHttpRequest source,Channel channel) {
        wrap(source);
        this.channel = Objects.requireNonNull(channel);
    }

    public Channel getChannel() {
        return channel;
    }

    public HttpMethod getMethod() {
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.getMethod();
        }
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
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.getUri();
        }
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
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.getProtocolVersion();
        }
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
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.getDecoderResult();
        }
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


    public FullHttpRequest touch() {
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.touch();
        }
        if(touchMethodList == null){
            synchronized (lock) {
                if(touchMethodList == null) {
                    touchMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "touch",0)
                    );
                }
            }
        }
        return (FullHttpRequest) ReflectUtil.invokeMethodOnce(source,touchMethodList);
    }

    public FullHttpRequest touch(Object hint) {
        if(!VersionConstants.isEnableVersionAdapter()){
            return source.touch(hint);
        }
        if(touch1MethodList == null){
            synchronized (lock) {
                if(touch1MethodList == null) {
                    touch1MethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "touch",1)
                    );
                }
            }
        }
        return (FullHttpRequest) ReflectUtil.invokeMethodOnce(source,touch1MethodList,hint);
    }

    public FullHttpRequest copy(ByteBuf newContent) {
//        if(!VersionConstants.isEnableVersionAdapter()){
//            return source.copy(newContent);
//        }
        if(copyMethodList == null){
            synchronized (lock) {
                if(copyMethodList == null) {
                    copyMethodList = Arrays.asList(
                            ReflectUtil.getAccessibleMethodByName(sourceClass, "copy",1)
                    );
                }
            }
        }
        return (FullHttpRequest) ReflectUtil.invokeMethodOnce(source,copyMethodList,newContent);
    }

    public FullHttpRequest duplicate() {
        HttpContent httpContent =  source.duplicate();
        if(httpContent instanceof FullHttpRequest){
            return (FullHttpRequest) httpContent;
        }
        return null;
    }

    public FullHttpRequest retainedDuplicate() {
        return source.retainedDuplicate();
    }

    public FullHttpRequest replace(ByteBuf byteBuf) {
        return source.replace(byteBuf);
    }

    @Override
    public FullHttpRequest setMethod(HttpMethod method) {
         source.setMethod(method);
         return this;
    }

    @Override
    public FullHttpRequest setUri(String uri) {
         source.setUri(uri);
         return this;
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return source.trailingHeaders();
    }

    @Override
    public ByteBuf content() {
        return source.content();
    }

    @Override
    public FullHttpRequest copy() {
        return source.copy();
    }

    @Override
    public FullHttpRequest retain(int increment) {
        source.retain(increment);
        return this;
    }

    @Override
    public boolean release() {
        return source.release();
    }

    @Override
    public boolean release(int decrement) {
        return source.release(decrement);
    }

    @Override
    public int refCnt() {
        return source.refCnt();
    }

    @Override
    public FullHttpRequest retain() {
         source.retain();
         return this;
    }

    @Override
    public FullHttpRequest setProtocolVersion(HttpVersion version) {
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
    public void wrap(FullHttpRequest source) {
        Objects.requireNonNull(source);
        this.source = source;
        this.sourceClass = source.getClass();
    }

    @Override
    public FullHttpRequest unwrap() {
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
