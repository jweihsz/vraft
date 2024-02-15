package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/15 15:21
 **/
public interface RpcRemoting {
    
    void unregisterProcessor(String uid);

    RpcProcessor<?> getProcessor(Object uid);

    void registerProcessor(String uid, RpcProcessor<?> processor);
}
