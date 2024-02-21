package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
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
    private final RpcActor defaultActor;
    private final Map<Long, Channel> connects;
    private final Map<ByteBuf, RpcProcessor> processor;
    private final Map<ByteBuf, ActorProcessor<RpcCmd>> actor;

    public RpcManager(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.actor = new ConcurrentHashMap<>();
        this.connects = new ConcurrentHashMap<>();
        this.processor = new ConcurrentHashMap<>();
        this.defaultActor = new RpcActor(sysCtx, null);
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
        if (old != null) {old.close();}
        UidService uid = sysCtx.getUidService();
        ch.attr(RpcCommon.CH_KEY).set(userId);
        ch.attr(RpcCommon.CH_R_ACTOR).set(uid.genActorId());
        ch.attr(RpcCommon.CH_W_ACTOR).set(uid.genActorId());
        ch.attr(RpcCommon.CH_PEND).set(new ConcurrentHashMap<>());
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

    public void removeProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        ByteBuf bf = RpcCommon.convert(uid);
        actor.remove(bf);
        processor.remove(bf);
    }

    public RpcProcessor getProcessor(Object uid) {
        if (!(uid instanceof ByteBuf)) {return null;}
        return processor.get((ByteBuf)uid);
    }

    public void addProcessor(String uid, RpcProcessor rp) {
        if (uid == null || uid.isEmpty() || rp == null) {return;}
        final ByteBuf bf = RpcCommon.convert(uid);
        processor.put(bf, rp);
        actor.put(bf, new RpcActor(sysCtx, bf));
    }

    public Object getActor(Object uid) {
        if (!(uid instanceof ByteBuf)) {return null;}
        Object obj = actor.get((ByteBuf)uid);
        return obj != null ? obj : defaultActor;
    }

    private class RpcActor implements ActorProcessor<RpcCmd> {
        private final ByteBuf uid;
        private final SystemCtx sysCtx;

        public RpcActor(SystemCtx sysCtx, ByteBuf uid) {
            this.uid = uid;
            this.sysCtx = sysCtx;
        }

        @Override
        public void process(long deadline, Actor<RpcCmd> self) {
            do {
                RpcCmd cmd = self.getQueue().peek();
                if (cmd == null) {break;}
                if (uid == null) {

                } else {
                    RpcProcessor p = processor.get(uid);
                    if (p != null) {
                        // p.handle(cmd.getUserId(), cmd.getMsgId(), cmd.getHeader(), cmd.getBody());
                    }
                }
            } while (System.currentTimeMillis() <= deadline);
        }
    }

}
