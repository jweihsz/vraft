package com.vraft.facade.system;

import com.vraft.facade.actor.ActorService;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.serializer.Serializer;
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
    private ConfigServer cfgServer;
    private RpcManager rpcManager;
    private RpcClient rpcClient;
    private UidService uidService;
    private RaftNodeGroup nodeGroup;
    private TimerService timerService;
    private ActorService actorService;
    private Serializer serializer;
    private SerializerMgr serializerMgr;
}
