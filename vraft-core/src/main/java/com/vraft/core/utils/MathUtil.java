package com.vraft.core.utils;

/**
 * @author jweih.hjw
 * @version 2024/2/5 14:47
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

    public static long address2long(String ip, int port) {
        long res = 0L;
        RequireUtil.isTrue(port > 0);
        RequireUtil.isTrue(ip != null && !ip.isEmpty());
        final String[] ss = ip.split("\\.");
        for (String str : ss) {
            res = res << 8L | Long.parseLong(str);
        }
        return res << 32L | port;
    }

    public static long address2long(String address) {
        String[] ss = address.split(":");
        return address2long(ss[0], Integer.parseInt(ss[1]));
    }

}
