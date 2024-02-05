package com.vraft.common;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 2:46 下午
 */
public class SystemUtil {
    private SystemUtil() {}

    public static int getPhyCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }
    
}
