package com.github.netty.util;

import com.github.netty.core.adapter.NettyHttpCookie;
import com.github.netty.core.constants.HttpHeaderConstants;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.concurrent.FastThreadLocal;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletUtil {

    private static final Map<String,HttpDataFactory> HTTP_DATA_FACTORY_MAP = new ConcurrentHashMap<>();

    private static final Method COOKIE_DECODER_METHOD;
    private static final Method COOKIE_ENCODER_METHOD;

    /**
     * The only date format permitted when generating HTTP headers.
     */
    private static final String RFC1123_DATE =
            "EEE, dd MMM yyyy HH:mm:ss zzz";

    private static final SimpleDateFormat[] FORMATS_TEMPLATE = {
            new SimpleDateFormat(RFC1123_DATE, Locale.ENGLISH),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.ENGLISH)
    };

    public static boolean isLocalhost(String host){
        return "0:0:0:0:0:0:0:1".equals(host) || "locahost".equals(host);
    }

    /**
     * SimpleDateFormat非线程安全，为了节省内存提高效率，把他放在ThreadLocal里
     * 用于设置HTTP响应头的时间信息
     */
    private static final FastThreadLocal<DateFormat> HTTP_DATE_FORMAT = new FastThreadLocal<DateFormat>() {
        private TimeZone timeZone = TimeZone.getTimeZone("GMT");
        @Override
        protected DateFormat initialValue() {
            DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH);
            df.setTimeZone(timeZone);
            return df;
        }
    };

    static {
        Class cookieDecoderClass = ReflectUtil.forName(
                "io.netty.handler.codec.http.CookieDecoder",
                "io.netty.handler.codec.http.ServerCookieDecoder"
        );
        Class cookieEncoderClass = ReflectUtil.forName(
                "io.netty.handler.codec.http.CookieEncoder",
                "io.netty.handler.codec.http.ServerCookieEncoder"
        );

        if(cookieDecoderClass == null || cookieEncoderClass == null) {
            throw new RuntimeException("netty版本不兼容");
        }

        try {
            COOKIE_DECODER_METHOD = cookieDecoderClass.getDeclaredMethod("decode",String.class);
            if(COOKIE_DECODER_METHOD == null){
                throw new RuntimeException("netty版本不兼容");
            }

            COOKIE_ENCODER_METHOD = cookieEncoderClass.getDeclaredMethod("encode", io.netty.handler.codec.http.Cookie.class);
            if(COOKIE_ENCODER_METHOD == null){
                throw new RuntimeException("netty版本不兼容");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("netty版本不兼容");
        }
    }

    public static String getCookieValue(Cookie[] cookies, String cookieName){
        if(cookies == null || cookieName == null) {
            return null;
        }
        for(Cookie cookie : cookies){
            if(cookie == null) {
                continue;
            }

            String name = cookie.getName();
            if(cookieName.equals(name)){
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void decodeByUrl(Map<String,String[]> parameterMap, String uri, Charset charset){
        QueryStringDecoder decoder = new QueryStringDecoder(uri,charset);

        Map<String, List<String>> parameterListMap = decoder.parameters();
        for(Map.Entry<String,List<String>> entry : parameterListMap.entrySet()){
            List<String> value = entry.getValue();
            parameterMap.put(entry.getKey(), value.toArray(new String[value.size()]));
        }
    }

    public static String decodeCharacterEncoding(String contentType) {
        if (contentType == null) {
            return null;
        }
        int start = contentType.indexOf(HttpHeaderConstants.CHARSET+"=");
        if (start < 0) {
            return null;
        }
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0) {
            encoding = encoding.substring(0, end);
        }
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
                && (encoding.endsWith("\""))) {
            encoding = encoding.substring(1, encoding.length() - 1);
        }
        return encoding.trim();
    }

    public static String encodeCookie(io.netty.handler.codec.http.Cookie cookie){
        String value = (String) ReflectUtil.invokeMethod(null,COOKIE_ENCODER_METHOD,cookie);
        return value;
    }

    public static Cookie[] decodeCookie(String value){
        if(value == null){
            return null;
        }
        Collection<io.netty.handler.codec.http.Cookie> nettyCookieSet = (Collection<io.netty.handler.codec.http.Cookie>) ReflectUtil.invokeMethod(null,COOKIE_DECODER_METHOD,value);
        if(nettyCookieSet == null){
            return null;
        }

        io.netty.handler.codec.http.Cookie[] nettyCookieArr = nettyCookieSet.toArray(new io.netty.handler.codec.http.Cookie[nettyCookieSet.size()]);
        int size = nettyCookieArr.length;
        Cookie[] cookies = new Cookie[size];

        NettyHttpCookie nettyHttpCookie = new NettyHttpCookie();
        for (int i=0; i< size; i++) {
            io.netty.handler.codec.http.Cookie nettyCookie = nettyCookieArr[i];
            if(nettyCookie == null){
                continue;
            }

            nettyHttpCookie.wrap(nettyCookie);
            cookies[i] = toServletCookie(nettyHttpCookie);
        }
        return cookies;
    }

    public static Cookie toServletCookie(NettyHttpCookie nettyCookie){
        Cookie cookie = new Cookie(nettyCookie.getName(), nettyCookie.getValue());
        String comment = nettyCookie.getComment();
        if(comment != null) {
            cookie.setComment(comment);
        }
        String domain = nettyCookie.getDomain();
        if(domain != null) {
            cookie.setDomain(domain);
        }
        cookie.setHttpOnly(nettyCookie.isHttpOnly());
        cookie.setMaxAge((int) nettyCookie.getMaxAge());
        cookie.setPath(nettyCookie.getPath());
        cookie.setVersion(nettyCookie.getVersion());
        cookie.setSecure(nettyCookie.isSecure());
        return cookie;
    }

    public static io.netty.handler.codec.http.Cookie toNettyCookie(Cookie cookie){
        io.netty.handler.codec.http.Cookie nettyCookie = new DefaultCookie(cookie.getName(),cookie.getValue());
        String comment = cookie.getComment();
        if(comment != null) {
            nettyCookie.setComment(comment);
        }
        String domain = cookie.getDomain();
        if(domain != null) {
            nettyCookie.setDomain(domain);
        }
        nettyCookie.setHttpOnly(nettyCookie.isHttpOnly());
        nettyCookie.setMaxAge(cookie.getMaxAge());
        nettyCookie.setPath(cookie.getPath());
        nettyCookie.setVersion(cookie.getVersion());
        nettyCookie.setSecure(cookie.getSecure());

        return nettyCookie;
    }

    public static void decodeByBody(Map<String,String[]> parameterMap,HttpRequest httpRequest,Charset charset){
        HttpDataFactory factory = getHttpDataFactory(charset);
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, httpRequest,charset);

        while (decoder.hasNext()){
            InterfaceHttpData interfaceData = decoder.next();
            /*
             * HttpDataType有三种类型
             * Attribute, FileUpload, InternalAttribute
             */
            switch (interfaceData.getHttpDataType()){
                case Attribute:{
                    Attribute data = (Attribute) interfaceData;
                    String name = data.getName();
                    String value;
                    try {
                        value = data.getValue();
                    } catch (IOException e) {
                        e.printStackTrace();
                        value = "";
                    }
                    parameterMap.put(name, new String[]{value});
                    break;
                }
                case FileUpload:{
                    FileUpload data = (FileUpload) interfaceData;

                    break;
                }
                case InternalAttribute:{
//                    InternalAttribute data = (InternalAttribute) interfaceData;

                    break;
                }
                default:{

                    break;
                }
            }

        }
        decoder.destroy();
    }

    public static Long parseHeaderDate(String value) {
        DateFormat[] formats = FORMATS_TEMPLATE;
        Date date = null;
        for (int i = 0; (date == null) && (i < formats.length); i++) {
            try {
                date = formats[i].parse(value);
            } catch (ParseException e) {
                // Ignore
            }
        }
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    private static HttpDataFactory getHttpDataFactory(Charset charset){
        String charsetName = charset.name();
        HttpDataFactory factory = HTTP_DATA_FACTORY_MAP.get(charsetName);
        if(factory == null){
            synchronized (HTTP_DATA_FACTORY_MAP) {
                factory = HTTP_DATA_FACTORY_MAP.get(charsetName);
                if(factory == null) {
                    //Disk false
                    factory = new DefaultHttpDataFactory(false, charset);
                    HTTP_DATA_FACTORY_MAP.put(charsetName, factory);
                }
            }
        }
        return factory;
    }

    /**
     * @return 线程安全的获取当前时间格式化后的字符串
     */
    public static String newDateGMT() {
        return HTTP_DATE_FORMAT.get().format(new Date());
    }

}
