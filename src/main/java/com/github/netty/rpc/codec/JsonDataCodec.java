package com.github.netty.rpc.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;
import com.github.netty.core.constants.CoreConstants;
import com.github.netty.rpc.exception.RpcDecodeException;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author 84215
 */
public class JsonDataCodec implements DataCodec {

    private static final byte[] EMPTY = new byte[0];
    private static final SerializerFeature[] SERIALIZER_FEATURES = {
            SerializerFeature.WriteClassName
    };

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private ParserConfig parserConfig;

    public JsonDataCodec() {
        this.parserConfig = new ParserConfig();
    }

    @Override
    public byte[] encodeRequestData(Object[] data) {
        if(data == null || data.length == 0){
            return EMPTY;
        }

        if(CoreConstants.isEnableExecuteHold()){
            return CoreConstants.holdExecute(() -> {
                return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
            });
        }

        return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
    }

    @Override
    public Object[] decodeRequestData(String data) {
        if(data == null || data.isEmpty()){
            return null;
        }

        if(CoreConstants.isEnableExecuteHold()){
            return CoreConstants.holdExecute(() -> {
                List list = (List) JSON.parse(data,parserConfig);
                return list.toArray();
            });
        }

        List list = (List) JSON.parse(data,parserConfig);
        return list.toArray();
    }

    @Override
    public Object[] decodeRequestData(byte[] data) {
        if(data == null || data.length == 0){
            return null;
        }

        if(CoreConstants.isEnableExecuteHold()){
            return CoreConstants.holdExecute(() -> {
                List list = (List) JSON.parse(data,0,data.length,UTF8.newDecoder(),JSON.DEFAULT_PARSER_FEATURE);
                return list.toArray();
            });
        }

        List list = (List) JSON.parse(data,0,data.length,UTF8.newDecoder(),JSON.DEFAULT_PARSER_FEATURE);
        return list.toArray();
    }

    @Override
    public byte[] encodeResponseData(Object data) {
        if(data == null){
            return EMPTY;
        }

        if(CoreConstants.isEnableExecuteHold()){
            return CoreConstants.holdExecute(() -> {
                return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
            });
        }

        return JSON.toJSONBytes(data,SERIALIZER_FEATURES);
    }

    @Override
    public Object decodeResponseData(byte[] data) {
        if(data == null || data.length == 0){
            return null;
        }
        if(CoreConstants.isEnableExecuteHold()){
            return CoreConstants.holdExecute(() -> {
                return JSON.parse(data);
            });
        }

        return JSON.parse(data);
    }

    @Override
    public <T> T cast(Object data, Class<T> type) {
        try {
            if(CoreConstants.isEnableExecuteHold()){
                return CoreConstants.holdExecute(() -> {
                    return TypeUtils.cast(data,type,parserConfig);
                });
            }

            return TypeUtils.cast(data,type,parserConfig);
        }catch (Exception e){
            throw new RpcDecodeException(e.getMessage(),e);
        }
    }

}
