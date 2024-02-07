package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/7 16:20
 **/
public interface RpcProcessor<T> {

    String uid();

    Object handle(RpcCtx ctx, T req) throws Exception;
}
