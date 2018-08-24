package com.github.netty.core.rpc.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;
import com.github.netty.core.rpc.exception.RpcDecodeException;

import java.util.List;

/**
 * @author 84215
 */
public class JsonDataCodec implements DataCodec {

    private static final SerializerFeature[] SERIALIZER_FEATURES = {
            SerializerFeature.WriteClassName
    };

    private ParserConfig parserConfig;

    public JsonDataCodec() {
        this.parserConfig = new ParserConfig();
    }
    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] encodeRequestData(Object[] data) {
        if(data == null || data.length == 0){
            return EMPTY;
        }
        return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
    }

    @Override
    public Object[] decodeRequestData(byte[] data) {
        if(data == null || data.length == 0){
            return null;
        }
        List list = (List)JSON.parse(data);
        return list.toArray();
    }

    @Override
    public byte[] encodeResponseData(Object data) {
        if(data == null){
            return EMPTY;
        }
        return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
    }

    @Override
    public Object decodeResponseData(byte[] data) {
        if(data == null || data.length == 0){
            return null;
        }
        return JSON.parse(data);
    }

    @Override
    public <T> T cast(Object data, Class<T> type) {
        try {

        return TypeUtils.cast(data,type,parserConfig);
        }catch (Exception e){
            throw new RpcDecodeException(e.getMessage(),e);
        }
    }

}
