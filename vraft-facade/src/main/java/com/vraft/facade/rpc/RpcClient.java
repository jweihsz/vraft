package com.vraft.facade.rpc;

import com.vraft.facade.common.CallBack;
import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/13 10:32
 **/
public interface RpcClient extends LifeCycle {

    long doConnect(String host);

    boolean oneWay(long userId, long msgId, byte biz, long groupId,
        long nodeId, String uid, byte[] header, byte[] body) throws Exception;

    boolean twoWay(long userId, byte biz, long groupId,
        long nodeId, String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception;

    boolean resp(long userId, byte biz, long groupId,
        long nodeId, long msgId, String uid,
        byte[] header, byte[] body) throws Exception;
}
