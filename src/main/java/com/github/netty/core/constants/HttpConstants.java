package com.github.netty.core.constants;

import java.nio.charset.Charset;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class HttpConstants {

    public static final int SESSION_ID_SOURCE_COOKIE = 1;
    public static final int SESSION_ID_SOURCE_URL = 2;
    public static final int SESSION_ID_SOURCE_NOT_FOUND_CREATE = 3;
    public static final String JSESSION_ID_COOKIE = "JSESSIONID";
    public static final String JSESSION_ID_PARAMS = "jsessionid";

    public static final String SP = ",";
    public static final String POST = "POST";
    public static final String HTTPS = "https";
    public static final int HTTPS_PORT = 443;
    public static final int HTTP_PORT = 80;
    public static final String HTTP = "http";
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

}
