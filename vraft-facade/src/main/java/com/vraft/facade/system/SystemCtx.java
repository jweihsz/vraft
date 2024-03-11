package com.vraft.facade.system;

import com.vraft.facade.actor.ActorService;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.raft.node.RaftAllGroup;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.rpc.RpcServer;
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
    private UidService uidSvs;
    private ConfigServer cfgSvs;
    private RpcManager rpcMgr;
    private RpcServer rpcSrv;
    private RpcClient rpcClient;
    private TimerService timerSvs;
    private ActorService actorSvs;
    private RaftAllGroup nodeGroupSrv;
    private SerializerMgr serializerMgr;
}
