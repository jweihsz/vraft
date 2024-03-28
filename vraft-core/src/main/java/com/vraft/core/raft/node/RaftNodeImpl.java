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
import com.vraft.facade.raft.fsm.FsmCallback;
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
    private final RaftNodeCtx ctx;

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeMate self) {
        this.sysCtx = sysCtx;
        this.ctx = new RaftNodeCtx();
        this.ctx.setSelf(self);
    }

    @Override
    public void shutdown() {}

    @Override
    public void init() throws Exception {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
        RequireUtil.nonNull(sysCtx.getTimerSvs());

        RaftNodeMate self = ctx.getSelf();
        RequireUtil.nonNull(self);
        RequireUtil.isTrue(self.getGroupId() > 0);
        RequireUtil.nonNull(self.getSrcIp());

        ctx.setElectTimeout(1000);
        ctx.setMaxElectTimeout(2000);

        RaftNodeMate mate = ctx.getSelf();
        mate.setRole(RaftNodeStatus.FOLLOWER);
        mate.setLastLeaderHeat(-1L);
        mate.setNodeId(MathUtil.address2long(mate.getSrcIp()));

        long t = System.currentTimeMillis();
        ctx.setEpoch(new AtomicLong(t));

        ctx.setPeersMgr(new RaftPeersMgrImpl(sysCtx));
        ctx.getPeersMgr().init();

        ctx.setLogsMgr(new RaftLogsMgrImpl(sysCtx));
        ctx.getLogsMgr().init();

        ctx.setElectMgr(new RaftElectMgrImpl(sysCtx, this));
        ctx.getElectMgr().init();

        initSerializer();

        sysCtx.getRaftNodeMgr().registerNode(this);
    }

    @Override
    public void startup() throws Exception {
        ctx.getElectMgr().startVote(true);
    }

    @Override
    public RaftNodeCtx getNodeCtx() {return ctx;}

    private void initSerializer() {
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        sz.registerClz(Arrays.asList(
            byte[].class, RaftVoteReq.class,
            RaftInnerCmd.class, RaftVoteResp.class)
        );
    }

    @Override
    public void checkReplicator(long nodeId) {
        final RaftNodeMate mate = ctx.getSelf();
        if (mate.getRole() != RaftNodeStatus.LEADER) {return;}
        //TODO
    }

    private void resetLeaderId(long leaderId) {
        RaftNodeMate self = ctx.getSelf();
        long oldLeaderId = self.getLeaderId();
        FsmCallback fsm = ctx.getFsmCallback();
        self.setLeaderId(leaderId);
        if (fsm == null) {return;}
        if (oldLeaderId > 0 && leaderId < 0) {
            fsm.onStopFollowing(oldLeaderId, self.getCurTerm());
        } else if (oldLeaderId < 0 && leaderId > 0) {
            fsm.onStartFollowing(leaderId, self.getCurTerm());
        }
    }

}
