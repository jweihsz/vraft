package com.vraft.facade.common;

/**
 * @author jweihsz
 * @version 2024/2/8 23:19
 **/
public interface LifeCycle {

    default void shutdown() {}

    default String getName() {return "";}

    default void init() throws Exception {}

    default void startup() throws Exception {}
}
