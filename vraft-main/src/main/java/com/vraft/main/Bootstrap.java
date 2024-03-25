package com.vraft.main;

import java.util.concurrent.CountDownLatch;

import com.vraft.core.actor.ActorHolder;
import com.vraft.core.config.ConfigHolder;
import com.vraft.core.raft.handler.RaftVoteReqHandler;
import com.vraft.core.raft.handler.RaftVoteRespHandler;
import com.vraft.core.raft.node.RaftNodeImpl;
import com.vraft.core.raft.node.RaftNodeMgrImpl;
import com.vraft.core.rpc.RpcClientImpl;
import com.vraft.core.rpc.RpcManagerImpl;
import com.vraft.core.rpc.RpcServerImpl;
import com.vraft.core.serialize.SerializeHolder;
import com.vraft.core.timer.TimerHolder;
import com.vraft.core.uid.UidHolder;
import com.vraft.core.utils.MathUtil;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.config.RaftNodeCfg;
import com.vraft.facade.config.RpcClientCfg;
import com.vraft.facade.config.RpcServerCfg;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.rpc.RpcServer;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import com.vraft.facade.uid.UidService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bootstrap {
    private final static Logger logger = LogManager.getLogger(Bootstrap.class);

    private static final SystemCtx sysCtx = new SystemCtx();

    public static void main(String[] args) throws Exception {
        logger.info("hello vRaft!");
        CountDownLatch ct = new CountDownLatch(1);
        Bootstrap b = new Bootstrap().init();
        b.startup();
        ct.await();
    }

    private Bootstrap init() throws Exception {
        UidService uidSrv = new UidHolder();
        sysCtx.setUidSvs(uidSrv);

        ConfigServer cfg = new ConfigHolder(sysCtx);
        cfg.startup();
        sysCtx.setCfgSvs(cfg);

        SerializerMgr serializerMgr = new SerializeHolder();
        sysCtx.setSerializerMgr(serializerMgr);

        RaftNodeMgr raftNodeMgr = new RaftNodeMgrImpl(sysCtx);
        raftNodeMgr.init();
        sysCtx.setRaftNodeMgr(raftNodeMgr);

        TimerService timerSvs = new TimerHolder(sysCtx);
        timerSvs.startup();
        sysCtx.setTimerSvs(timerSvs);

        RpcManager rpcMgr = new RpcManagerImpl(sysCtx);
        rpcMgr.addProcessor(new RaftVoteReqHandler(sysCtx));
        rpcMgr.addProcessor(new RaftVoteRespHandler(sysCtx));
        rpcMgr.startup();
        sysCtx.setRpcMgr(rpcMgr);

        RpcClientCfg clientCfg = cfg.getRpcClientCfg();
        RpcClient rpcClient = new RpcClientImpl(sysCtx, clientCfg);
        rpcClient.startup();
        sysCtx.setRpcClient(rpcClient);

        RpcServerCfg srvCfg = cfg.getRpcServerCfg();
        RpcServer rpcServer = new RpcServerImpl(sysCtx, srvCfg);
        rpcServer.startup();
        sysCtx.setRpcSrv(rpcServer);

        ActorService actorSrv = new ActorHolder(sysCtx);
        sysCtx.setActorSvs(actorSrv);

        return this;

    }

    private void startup() throws Exception {
        ConfigServer cfgSrv = sysCtx.getCfgSvs();
        RaftNodeCfg cfg = cfgSrv.getRaftNodeCfg();
        RaftNodeMate self = new RaftNodeMate();
        self.setGroupId(1);
        self.setSrcIp(cfg.getRaftSelf());
        self.setNodeId(MathUtil.address2long(cfg.getRaftSelf()));
        RaftNode node = new RaftNodeImpl(sysCtx, self);
        node.init();
        node.startup();
    }

}
