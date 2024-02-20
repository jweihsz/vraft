package com.vraft.core.rpc;

import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.facade.system.SystemCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jweihsz
 * @version 2024/2/20 17:00
 **/
public class RpcActor implements ActorProcessor<RpcCmd> {
    private static final Logger logger = LoggerFactory.getLogger(RpcActor.class);

    private final SystemCtx sysCtx;

    public RpcActor(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public boolean process(long deadline, Actor<RpcCmd> self) {
        return false;
    }
}
