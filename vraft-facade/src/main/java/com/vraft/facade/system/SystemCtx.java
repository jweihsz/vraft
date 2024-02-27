package com.vraft.facade.system;

import com.vraft.facade.actor.ActorService;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.timer.TimerService;
import com.vraft.facade.uid.UidService;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/11 11:35
 **/
@Data
public class SystemCtx {
    private RpcManager rpcManager;
    private UidService uidService;
    private TimerService timerService;
    private ActorService actorService;
    private ConfigServer configServer;
    private SerializerMgr serializerMgr;
}
