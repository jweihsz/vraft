package com.vraft.facade.actor;

import com.vraft.facade.rpc.RpcCmd;

/**
 * @author jweihsz
 * @version 2024/2/21 15:45
 **/
public interface ActorService {

    boolean dispatchAsyncRsp(long userId, RpcCmd cmd);

    boolean dispatchWriteChMsg(long userId, RpcCmd cmd);

}
