package com.github.netty.util;

import com.google.common.net.MediaType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by acer01 on 2018/8/4/004.
 */
public class MediaTypeUtil {

    private static Map<String,MediaType> mediaTypeCacheMap = new ConcurrentHashMap<>();

    public static MediaType parse(String type){
        MediaType mediaType = mediaTypeCacheMap.get(type);
        if(mediaType == null) {
            mediaType = MediaType.parse(type);
            if(mediaType != null){
                mediaTypeCacheMap.put(type,mediaType);
            }
        }
        return mediaType;
    }


}
