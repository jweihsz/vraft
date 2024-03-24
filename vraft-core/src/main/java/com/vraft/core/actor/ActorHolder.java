package com.vraft.core.actor;

import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.core.pool.ThreadPool;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.rpc.RpcCmd;
import com.vraft.facade.system.SystemCtx;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jweihsz
 * @version 2024/2/21 14:20
 **/
public class ActorHolder implements ActorService {
    private static final Logger logger = LoggerFactory.getLogger(ActorHolder.class);

    private final SystemCtx sysCtx;
    private final ActorSystem actor;
    private final ActorProcessor<RpcCmd> actorWch;
    private final ActorProcessor<RpcCmd> actorRsp;
    private final ActorProcessor<ByteBuf> actorGroup;

    public ActorHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.actorGroup = new ActorRaftGroup(sysCtx);
        this.actorRsp = new ActorAsyncResp(sysCtx);
        this.actorWch = new ActorWriteChannel(sysCtx);
        this.actor = new ActorSystem(ThreadPool.ACTOR);
    }

    @Override
    public boolean dispatchWriteChMsg(long userId, RpcCmd cmd) {
        return actor.dispatch(userId, cmd, actorWch);
    }

    @Override
    public boolean dispatchAsyncRsp(long userId, RpcCmd cmd) {
        return actor.dispatch(userId, cmd, actorRsp);
    }

    @Override
    public boolean dispatchRaftGroup(long groupId, Object bf) {
        return actor.dispatch(groupId, (ByteBuf)bf, actorGroup);
    }

}
