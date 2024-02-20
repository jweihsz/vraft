package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/15 15:21
 **/
public interface RpcRemoting {

    void removeProcessor(String uid);

    RpcProcessor<?> getProcessor(Object uid);

    void addProcessor(String uid, RpcProcessor<?> rp);
}
