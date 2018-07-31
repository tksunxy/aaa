package com.github.netty.core.adapter;

import io.netty.util.Version;

import java.util.Map;

/**
 * @author 84215
 */
public class NettyVersion {

    private static Map<String,Version> nettyVersionMap;

    static {
        nettyVersionMap = Version.identify();
    }

    public static Map<String, Version> getNettyVersionMap() {
        return nettyVersionMap;
    }

}
