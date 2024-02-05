package com.vraft.common;

/**
 * @author jweih.hjw
 * @version 2024/2/5 14:46
 */
public class SystemUtil {
    private SystemUtil() {}

    public static int getPhyCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }

}
