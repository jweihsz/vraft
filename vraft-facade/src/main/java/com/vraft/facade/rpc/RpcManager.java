package com.vraft.facade.rpc;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/21 14:45
 **/
public interface RpcManager extends LifeCycle {

    long getUserId(Object channel);

    long getUserId(String host);

    Object getChannel(long userId);

    void removeChannel(long userId);

    void removeChannel(Object channel);

    void addChannel(long userId, Object channel);

    void removeProcessor(String uid);

    RpcProcessor getProcessor(Object uid);

    void addProcessor(RpcProcessor rp);

    Object removePendMsg(long userId, long msgId);

    Object removePendMsg(Object channel, long msgId);

    boolean addPendMsg(long userId, long msgId, Object obj);

    boolean addPendMsg(Object channel, long msgId, Object obj);

}
