package com.github.netty.core;

import com.github.netty.core.support.AbstractRecycler;
import com.github.netty.core.support.CompositeByteBufX;
import com.github.netty.core.support.Recyclable;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;

/**
 * 用于兼容 netty4 与netty5
 * @author acer01
 * 2018/7/28/028
 */
public class NettyHttpResponse implements FullHttpResponse,Recyclable {

    private static final AbstractRecycler<NettyHttpResponse> RECYCLER = new AbstractRecycler<NettyHttpResponse>() {
        @Override
        protected NettyHttpResponse newInstance() {
            return new NettyHttpResponse();
        }
    };

    private DecoderResult decoderResult;
    private HttpVersion version;
    private HttpHeaders headers;
    private HttpResponseStatus status;
    private CompositeByteBufX content;

    public NettyHttpResponse() {
        this.headers = new DefaultHttpHeaders(false);
        this.version = HttpVersion.HTTP_1_1;
        this.status = HttpResponseStatus.OK;
        this.decoderResult = DecoderResult.SUCCESS;
    }

    public static NettyHttpResponse newInstance() {
        NettyHttpResponse instance = RECYCLER.get();
        return instance;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return version;
    }

    @Override
    public DecoderResult getDecoderResult() {
        return decoderResult;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public HttpVersion protocolVersion() {
        return version;
    }

    @Override
    public DecoderResult decoderResult() {
        return decoderResult;
    }

    @Override
    public NettyHttpResponse setStatus(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return headers;
    }

    @Override
    public CompositeByteBufX content() {
        return content;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public FullHttpResponse retain() {
        content.retain();
        return this;
    }

    @Override
    public FullHttpResponse retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public FullHttpResponse touch() {
        content.touch();
        return this;
    }

    @Override
    public FullHttpResponse touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    @Override
    public NettyHttpResponse copy() {
        return replace(content().copy());
    }

    @Override
    public NettyHttpResponse duplicate() {
        return replace(content().duplicate());
    }

    @Override
    public NettyHttpResponse retainedDuplicate() {
        return replace(content().retainedDuplicate());
    }

    @Override
    public NettyHttpResponse replace(ByteBuf content) {
        NettyHttpResponse response = new NettyHttpResponse();
        if(content instanceof CompositeByteBufX){
             response.content = (CompositeByteBufX) content;
        }else {
            response.content = new CompositeByteBufX();
        }
        response.version = this.version;
        response.status = this.status;
        response.headers = this.headers.copy();
        response.decoderResult = this.decoderResult;
        return response;
    }

    @Override
    public NettyHttpResponse setProtocolVersion(HttpVersion version) {
        this.version = version;
        return this;
    }

    public void setContent(ByteBuf content) {
        if(content instanceof CompositeByteBufX){
            this.content = (CompositeByteBufX) content;
        }else {
            this.content = new CompositeByteBufX();
            this.content.addComponent(content);
        }
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        this.decoderResult = result;
    }

    @Override
    public void recycle() {
        this.content = null;
        this.decoderResult = null;
        this.version = null;
        this.headers = null;
        this.status = null;
        RECYCLER.recycle(this);
    }

    @Override
    public int hashCode() {
        int result = decoderResult != null ? decoderResult.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        NettyHttpResponse that = (NettyHttpResponse) o;
        if (!decoderResult.equals(that.decoderResult)){
            return false;
        }
        if (!version.equals(that.version)){
            return false;
        }
        if (!status.equals(that.status)){
            return false;
        }
        if (!headers.equals(that.headers)){
            return false;
        }
        return content.equals(that.content);
    }

    @Override
    public String toString() {
        return "NettyHttpResponse{" +
                "content=" + content +
                ", decoderResult=" + decoderResult +
                ", version=" + version +
                ", headers=" + headers +
                ", status=" + status +
                '}';
    }

}
