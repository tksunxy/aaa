package com.github.netty.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author acer01
 *  2018/7/22/022
 */
public class ObjectUtil {

    public static final Object NULL = new Object();

    public static void checkState(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3, Object p4) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, p2, p3, p4}));
        }
    }

    public static <T> T checkNotNull(T reference) {
        if(reference == null) {
            throw new NullPointerException();
        } else {
            return reference;
        }
    }


    public static Class[] add(Class[] arr,Class... classes) {
        List<Class> list = new ArrayList<>(Arrays.asList(arr));
        for(Class clazz : classes){
            if(clazz != null) {
                list.add(clazz);
            }
        }
        return list.toArray(new Class[list.size()]);
    }


    public static boolean isNotEmpty(String str){
        return !isEmpty(str);
    }

    public static boolean isEmpty(String str){
        return str == null || str.isEmpty();
    }

    static String format(String template, Object... args) {
        template = String.valueOf(template);
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;

        int i;
        int placeholderStart;
        for(i = 0; i < args.length; templateStart = placeholderStart + 2) {
            placeholderStart = template.indexOf("%s", templateStart);
            if(placeholderStart == -1) {
                break;
            }

            builder.append(template, templateStart, placeholderStart);
            builder.append(args[i++]);
        }

        builder.append(template, templateStart, template.length());
        if(i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);

            while(i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }

            builder.append(']');
        }

        return builder.toString();
    }

}
