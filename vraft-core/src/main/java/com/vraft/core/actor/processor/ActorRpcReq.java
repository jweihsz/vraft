package com.vraft.core.actor.processor;

import com.vraft.core.actor.base.ActorSystem.Actor;
import com.vraft.core.actor.base.ActorSystem.Processor;
import com.vraft.core.rpc.RpcCmd;
import com.vraft.facade.system.SystemCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jweihsz
 * @version 2024/2/19 13:51
 **/
public class ActorRpcReq implements Processor<RpcCmd> {
    private static final Logger logger = LoggerFactory.getLogger(ActorRpcReq.class);

    private final SystemCtx sysCtx;

    public ActorRpcReq(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public boolean process(long deadline, Actor<RpcCmd> self) {
        return false;
    }
}