package com.github.netty.util;

import java.net.NetworkInterface;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.StringJoiner;

/**
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * Common functions that involve the host platform
 * @author 84215
 */
public class HostUtil {
    private static String osName;
    private static String osArch;
    private static boolean embedded;
    private static boolean is64bit = false;

    static {
        embedded = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            osName = System.getProperty("os.name").toLowerCase();
            osArch = System.getProperty("os.arch").toLowerCase();

            is64bit =  osArch.equals("x64")
                    || osArch.equals("x86_64")
                    || osArch.equals("ia64");

            return Boolean.getBoolean("com.sun.javafx.isEmbedded");
        });
    }

    public static String getOsName() {
        return osName;
    }

    public static boolean is64Bit() {
        return is64bit;
    }

    public static boolean isWindows() {
        return osName.startsWith("windows");
    }

    public static boolean isMacOSX() {
        return osName.startsWith("mac os x");
    }

    public static boolean isLinux() {
        return osName.startsWith("linux");
    }

    public static boolean isIOS() {
        return osName.startsWith("ios");
    }


    /**
     * Returns true if the platform is embedded.
     * @return 是否虚拟容器
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    /**
     * mac地址
     * @return 例( ‎00-24-7E-0A-22-93)
     */
    public static String getMac() {
        try {
            Enumeration<NetworkInterface> el = NetworkInterface
                    .getNetworkInterfaces();
            while (el.hasMoreElements()) {
                byte[] mac = el.nextElement().getHardwareAddress();
                if (mac == null || mac.length <= 0) {
                    continue;
                }
                String hexstr = bytesToHexString(mac);
                return getSplitString(hexstr, "-", 2).toUpperCase();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private static String getSplitString(String str, String split, int length) {
        if(str == null || split.length() == 0){
            return "";
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i % length == 0 && i > 0) {
                sb.append(split);
            }
            sb.append(str.charAt(i));
        }

        String[] attrs = sb.toString().split(split);
        StringJoiner stringJoiner = new StringJoiner(split);
        for (String attr : attrs) {
            stringJoiner.add(attr);
        }
        return stringJoiner.toString();
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");

        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}
