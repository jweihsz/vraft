package com.vraft.facade.rpc;

import com.vraft.facade.common.CallBack;
import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/2/13 10:32
 **/
public interface RpcClient extends LifeCycle {

    Object doConnect(String host) throws Exception;

    boolean oneWay(long userId, String uid, byte[] header,
        byte[] body) throws Exception;

    boolean twoWay(long userId, String uid, byte[] header,
        byte[] body, long timeout, CallBack cb) throws Exception;

    boolean resp(SystemCtx ctx, long userId, long msgId, String uid,
        byte[] header, byte[] body) throws Exception;
}
