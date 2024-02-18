package com.vraft.facade.common;

/**
 * @author jweihsz
 * @version 2024/2/18 14:34
 **/
public interface CallBack {
    void run(Object header, Object body, Throwable e);
}
