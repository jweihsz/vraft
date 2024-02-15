package com.vraft.facade.rpc;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/13 10:32
 **/
public interface RpcClient extends LifeCycle {

    void unregisterProcessor(String uid);

    RpcProcessor<?> getProcessor(Object uid);

    void registerProcessor(String uid, RpcProcessor<?> processor);
}
