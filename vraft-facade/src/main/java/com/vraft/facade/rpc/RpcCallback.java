package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/15 15:30
 **/
public interface RpcCallback {

    default void resp(RpcCtx ctx, RpcCommand rsp) {}
}
