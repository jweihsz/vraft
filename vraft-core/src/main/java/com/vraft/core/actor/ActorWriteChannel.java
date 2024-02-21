package com.vraft.core.actor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.facade.rpc.RpcCmd;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/21 14:23
 **/
public class ActorWriteChannel implements ActorProcessor<RpcCmd> {
    private final static Logger logger = LogManager.getLogger(ActorWriteChannel.class);

    private final SystemCtx sysCtx;
    private final Map<Long, Long> maps;

    public ActorWriteChannel(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public void process(long deadline, Actor<RpcCmd> self) {

    }

    @Override
    public long actorId(long userId, ByteBuf uid) {
        Long actorId = maps.get(userId);
        if (actorId != null) {return actorId;}
        UidService id = sysCtx.getUidService();
        actorId = maps.put(userId, id.genActorId());
        return actorId == null ? maps.get(userId) : actorId;
    }

}
