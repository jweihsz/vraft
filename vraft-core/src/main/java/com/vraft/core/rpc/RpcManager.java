package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.system.SystemCtx;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/7 17:31
 **/
public class RpcManager {
    private final static Logger logger = LogManager.getLogger(RpcManager.class);

    private final SystemCtx sysCtx;
    private final Map<Long, Channel> connects;

    public RpcManager(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.connects = new ConcurrentHashMap<>();
    }

    public Channel getChannel(long userId) {
        return connects.get(userId);
    }

    public void removeChannel(long userId) {
        connects.remove(userId);
    }

    public void removeChannel(Channel ch) {
        long userId = ch.attr(RpcCommon.CH_KEY).get();
        connects.remove(userId);
    }

    public void addChannel(long userId, Channel ch) {
        Channel old = connects.put(userId, ch);
        ch.attr(RpcCommon.CH_KEY).set(userId);
        ch.attr(RpcCommon.CH_PEND).set(new ConcurrentHashMap<>());
        if (old != null) {old.close();}
    }

    public Object removePendMsg(Channel ch, long msgId) {
        if (ch == null) {return null;}
        return ch.attr(RpcCommon.CH_PEND).get().remove(msgId);
    }

    public Object removePendMsg(long userId, long msgId) {
        final Channel ch = connects.get(userId);
        if (ch == null) {return null;}
        return removePendMsg(ch, msgId);
    }

    public boolean addPendMsg(Channel ch, long msgId, Object obj) {
        if (ch == null) {return false;}
        ch.attr(RpcCommon.CH_PEND).get().put(msgId, obj);
        return true;
    }

    public boolean addPendMsg(long userId, long msgId, Object obj) {
        final Channel ch = connects.get(userId);
        if (ch == null) {return false;}
        return addPendMsg(ch, msgId, obj);
    }
}
