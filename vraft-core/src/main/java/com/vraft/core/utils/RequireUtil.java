package com.vraft.core.utils;

/**
 * @author jweihsz
 * @version 2024/3/11 11:49
 **/
public class RequireUtil {
    private RequireUtil() {}

    public static <T> T nonNull(T obj) {
        if (obj != null) {return obj;}
        throw new NullPointerException();
    }

    public static <T> T nonNull(T obj, String message) {
        if (obj != null) {return obj;}
        throw new NullPointerException(message);
    }

    public static void isTrue(boolean exp) {
        if (exp) {return;}
        throw new IllegalArgumentException();
    }

    public static void isTrue(boolean exp, Object msg) {
        if (exp) {return;}
        throw new IllegalArgumentException(String.valueOf(msg));
    }
}
