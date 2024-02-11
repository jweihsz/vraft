package com.vraft.facade.rpc;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/7 16:25
 **/
public interface RpcService extends LifeCycle {

    default void unregisterProcessor(String uid) {}

    default RpcProcessor<?> getProcessor(String uid) {return null;}

    default void registerProcessor(String uid, RpcProcessor<?> processor) {}
}
