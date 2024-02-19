package com.vraft.core.actor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.actor.base.ActorSystem;
import com.vraft.core.actor.base.ActorSystem.Processor;
import com.vraft.core.actor.processor.ActorRpcReq;
import com.vraft.core.actor.processor.ActorRpcResp;
import com.vraft.core.pool.ThreadPool;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.actor.ActorType;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 03:26
 **/
public class ActorHolder implements ActorService {
    private final static Logger logger = LogManager.getLogger(ActorHolder.class);

    private final SystemCtx sysCtx;
    private final ActorSystem actorSys;
    private final Map<ActorType, Processor<?>> maps;

    public ActorHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.maps = new ConcurrentHashMap<>();
        this.actorSys = new ActorSystem(ThreadPool.ACTOR);
    }

    @Override
    public void startup() throws Exception {
        registerProcessor();
    }

    @Override
    public void shutdown() {}

    @Override
    public <E> boolean dispatch(long actorId, ActorType type, E msg) {
        Processor<E> p = (Processor<E>)maps.get(type);
        if (p == null) {return false;}
        actorSys.dispatch(actorId, msg, p);
        return true;
    }

    private void registerProcessor() {
        maps.put(ActorType.RPC_REQUEST, new ActorRpcReq(sysCtx));
        maps.put(ActorType.RPC_RESPONSE, new ActorRpcResp(sysCtx));
    }
}
