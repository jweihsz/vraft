package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/7 17:31
 **/
public class RpcManagerImpl implements RpcManager {
    private final static Logger logger = LogManager.getLogger(RpcManagerImpl.class);

    private final SystemCtx sysCtx;
    private final Map<String, Long> address;
    private final Map<Long, Channel> connects;
    private final Map<ByteBuf, RpcProcessor> processor;

    public RpcManagerImpl(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.address = new ConcurrentHashMap<>();
        this.connects = new ConcurrentHashMap<>();
        this.processor = new ConcurrentHashMap<>();
    }

    @Override
    public long getUserId(String host) {
        return address.getOrDefault(host, -1L);
    }

    @Override
    public long getUserId(Object channel) {
        if (!(channel instanceof Channel)) {return -1L;}
        final Channel ch = (Channel)channel;
        return ch.attr(RpcCommon.CH_KEY).get();
    }

    @Override
    public Object getChannel(long userId) {
        return connects.get(userId);
    }

    @Override
    public void removeChannel(long userId) {
        final Channel ch = connects.get(userId);
        String host = ch.attr(RpcCommon.HOST_KEY).get();
        if (host != null) {address.remove(host);}
        connects.remove(userId);
    }

    @Override
    public void removeChannel(Object channel) {
        if (!(channel instanceof Channel)) {return;}
        final Channel ch = (Channel)channel;
        long userId = ch.attr(RpcCommon.CH_KEY).get();
        connects.remove(userId);
        String host = ch.attr(RpcCommon.HOST_KEY).get();
        if (host != null) {address.remove(host);}
    }

    @Override
    public void addChannel(long userId, String host, Object channel) {
        if (!(channel instanceof Channel)) {return;}
        final Channel ch = (Channel)channel;
        Channel old = connects.put(userId, ch);
        if (old != null) {old.close();}
        ch.attr(RpcCommon.CH_KEY).set(userId);
        ch.attr(RpcCommon.CH_PEND).set(new ConcurrentHashMap<>());
        ch.attr(RpcCommon.HOST_KEY).set(host);
        address.put(host, userId);
    }

    @Override
    public Object removePendMsg(Object channel, long msgId) {
        if (!(channel instanceof Channel)) {return null;}
        final Channel ch = (Channel)channel;
        return ch.attr(RpcCommon.CH_PEND).get().remove(msgId);
    }

    @Override
    public Object removePendMsg(long userId, long msgId) {
        final Channel ch = connects.get(userId);
        if (ch == null) {return null;}
        return removePendMsg(ch, msgId);
    }

    @Override
    public boolean addPendMsg(Object channel, long msgId, Object obj) {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        ch.attr(RpcCommon.CH_PEND).get().put(msgId, obj);
        return true;
    }

    @Override
    public boolean addPendMsg(long userId, long msgId, Object obj) {
        final Channel ch = connects.get(userId);
        if (ch == null) {return false;}
        return addPendMsg(ch, msgId, obj);
    }

    @Override
    public void removeProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        ByteBuf bf = RpcCommon.convert(uid);
        processor.remove(bf);
    }

    @Override
    public RpcProcessor getProcessor(Object uid) {
        if (!(uid instanceof ByteBuf)) {return null;}
        return processor.get((ByteBuf)uid);
    }

    @Override
    public void addProcessor(RpcProcessor rp) {
        final String uid = rp.uid();
        
        if (uid == null || uid.isEmpty()) {return;}
        final ByteBuf bf = RpcCommon.convert(uid);
        processor.put(bf, rp);
    }
}
