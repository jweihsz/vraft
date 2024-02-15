package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.rpc.RpcProcessor;
import io.netty.buffer.ByteBuf;

/**
 * @author jweihsz
 * @version 2024/2/12 10:00
 **/
public abstract class RpcAbstract {
    private final Map<ByteBuf, RpcProcessor<?>> PROCESSOR = new ConcurrentHashMap<>();

    public void unregisterProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        PROCESSOR.remove(RpcCommon.convert(uid));
    }

    public RpcProcessor<?> getProcessor(Object uid) {
        if (uid instanceof ByteBuf) {
            return PROCESSOR.get((ByteBuf)uid);
        } else {
            return null;
        }
    }

    public void registerProcessor(String uid, RpcProcessor<?> processor) {
        if (uid == null || uid.isEmpty() || processor == null) {return;}
        final ByteBuf bf = RpcCommon.convert(uid);
        PROCESSOR.put(bf, processor);
    }

}
