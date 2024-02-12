package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.rpc.RpcProcessor;

/**
 * @author jweihsz
 * @version 2024/2/12 10:00
 **/
public abstract class RpcAbstract {
    private final Map<String, RpcProcessor<?>> PROCESSOR = new ConcurrentHashMap<>();

    public void unregisterProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        PROCESSOR.remove(uid);
    }

    public RpcProcessor<?> getProcessor(String uid) {
        return PROCESSOR.get(uid);
    }

    public void registerProcessor(String uid, RpcProcessor<?> processor) {
        if (uid == null || uid.isEmpty() || processor == null) {return;}
        PROCESSOR.put(uid, processor);
    }

}
