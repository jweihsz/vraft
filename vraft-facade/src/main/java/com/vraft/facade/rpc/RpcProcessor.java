package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/7 16:20
 **/
public interface RpcProcessor {

    String uid();

    void handle(long connectId, long msgId, byte[] header, byte[] body) throws Exception;
}
