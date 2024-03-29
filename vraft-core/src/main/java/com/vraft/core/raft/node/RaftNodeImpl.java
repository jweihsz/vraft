package com.vraft.core.raft.node;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.vraft.core.raft.elect.RaftElectMgrImpl;
import com.vraft.core.raft.logs.RaftLogsMgrImpl;
import com.vraft.core.raft.peers.RaftPeersMgrImpl;
import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeCtx;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/11 11:33
 **/
public class RaftNodeImpl implements RaftNode {
    private final static Logger logger = LogManager.getLogger(RaftNodeImpl.class);

    private final SystemCtx sysCtx;
    private final RaftNodeCtx nodeCtx;

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeMate self) {
        this.sysCtx = sysCtx;
        this.nodeCtx = new RaftNodeCtx();
        this.nodeCtx.setSelf(self);
    }

    @Override
    public void shutdown() {}

    @Override
    public void init() throws Exception {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
        RequireUtil.nonNull(sysCtx.getTimerSvs());

        RaftNodeMate self = nodeCtx.getSelf();
        RequireUtil.nonNull(self);
        RequireUtil.isTrue(self.getGroupId() > 0);
        RequireUtil.nonNull(self.getSrcIp());

        nodeCtx.setElectTimeout(1000);
        nodeCtx.setMaxElectTimeout(2000);

        RaftNodeMate mate = nodeCtx.getSelf();
        mate.setRole(RaftNodeStatus.FOLLOWER);
        mate.setLastLeaderHeat(-1L);
        mate.setNodeId(MathUtil.address2long(mate.getSrcIp()));

        long t = System.currentTimeMillis();
        nodeCtx.setEpoch(new AtomicLong(t));

        nodeCtx.setPeersMgr(new RaftPeersMgrImpl(sysCtx));
        nodeCtx.getPeersMgr().init();

        nodeCtx.setLogsMgr(new RaftLogsMgrImpl(sysCtx));
        nodeCtx.getLogsMgr().init();

        nodeCtx.setElectMgr(new RaftElectMgrImpl(sysCtx, this));
        nodeCtx.getElectMgr().init();

        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        sz.registerClz(Arrays.asList(
            byte[].class, RaftVoteReq.class,
            RaftInnerCmd.class, RaftVoteResp.class)
        );

        sysCtx.getRaftNodeMgr().registerNode(this);

    }

    @Override
    public void startup() throws Exception {
        RaftNodeMate self = nodeCtx.getSelf();
        nodeCtx.getElectMgr().doStepDown(self.getCurTerm());
    }

    @Override
    public RaftNodeCtx getNodeCtx() {return nodeCtx;}

    @Override
    public void checkReplicator(long nodeId) {
        RaftNodeMate mate = nodeCtx.getSelf();
        if (mate.getRole() != RaftNodeStatus.LEADER) {return;}
        //TODO
    }

}
