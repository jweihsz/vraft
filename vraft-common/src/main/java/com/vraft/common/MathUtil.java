package com.vraft.common;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 2:47 下午
 */
public class MathUtil {
    private MathUtil() {}

    public static int adjust2pow(int num) {
        int n = num - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1073741824) ? 1073741824 : n + 1;
    }
    
}
