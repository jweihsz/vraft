package com.vraft.core.actor;

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
 * @version 2024/2/21 15:25
 **/
public class ActorAsyncResp implements ActorProcessor<RpcCmd> {
    private final static Logger logger = LogManager.getLogger(ActorWriteChannel.class);

    private final int group = 16;
    private final Long[] actorIds;
    private final SystemCtx sysCtx;

    public ActorAsyncResp(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.actorIds = buildActorIds(group);
    }

    @Override
    public void process(long deadline, Actor<RpcCmd> self) {

    }

    @Override
    public long actorId(long userId, ByteBuf uid) {
        int index = (int)((userId) & (group - 1));
        return actorIds[index];
    }

    private Long[] buildActorIds(int group) {
        UidService id = sysCtx.getUidSvs();
        Long[] actors = new Long[group];
        for (int i = 0; i < group; i++) {
            actors[i] = id.genActorId();
        }
        return actors;
    }
}
