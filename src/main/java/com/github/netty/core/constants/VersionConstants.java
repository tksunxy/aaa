package com.github.netty.core.constants;

/**
 * Created by acer01 on 2018/8/5/005.
 */
public class VersionConstants {

    private static final boolean enableVersionAdapter;

    static {
        enableVersionAdapter = false;
    }

    public static boolean isEnableVersionAdapter() {
        return enableVersionAdapter;
    }

}
