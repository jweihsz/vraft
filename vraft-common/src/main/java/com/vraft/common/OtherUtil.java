package com.vraft.common;

/**
 * @author jweihsz
 * @version 2024/2/7 15:33
 **/
public class OtherUtil {
    private OtherUtil() {}

    public static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
}
