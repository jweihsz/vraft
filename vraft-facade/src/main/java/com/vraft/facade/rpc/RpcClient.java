package com.vraft.facade.rpc;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/13 10:32
 **/
public interface RpcClient extends LifeCycle, RpcRemoting {

    boolean registerUserId(Object ch);

    boolean unregisterUserId(Object ch);

    Object doConnect(String host) throws Exception;
}
