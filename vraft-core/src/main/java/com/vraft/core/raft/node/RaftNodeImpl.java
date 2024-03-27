package com.vraft.core.raft.node;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.vraft.core.raft.elect.RaftElectHolder;
import com.vraft.core.raft.peers.RaftPeersMgrImpl;
import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeOpts;
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
    private final RaftNodeOpts opts;

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeMate self) {
        this.sysCtx = sysCtx;
        this.opts = new RaftNodeOpts();
        this.opts.setSelf(self);
    }

    @Override
    public void shutdown() {}

    @Override
    public void init() throws Exception {
        validBase(sysCtx);
        validSelf(opts.getSelf());
        RaftNodeMate mate = opts.getSelf();
        mate.setRole(RaftNodeStatus.FOLLOWER);
        mate.setLastLeaderHeat(-1L);
        mate.setNodeId(MathUtil.address2long(mate.getSrcIp()));

        long t = System.currentTimeMillis();
        opts.setEpoch(new AtomicLong(t));

        opts.setRaftPeers(new RaftPeersMgrImpl(sysCtx));
        opts.getRaftPeers().init();

        opts.setRaftElect(new RaftElectHolder(sysCtx, this));

        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        sz.registerClz(Arrays.asList(RaftVoteReq.class,
            byte[].class, RaftInnerCmd.class, RaftVoteResp.class));

        sysCtx.getRaftNodeMgr().registerNode(this);
    }

    @Override
    public void startup() throws Exception {
        opts.getRaftElect().startVote(true);
    }

    @Override
    public RaftNodeOpts getOpts() {return opts;}

    public void validSelf(RaftNodeMate mate) {
        RequireUtil.nonNull(mate);
        RequireUtil.isTrue(mate.getGroupId() > 0);
        RequireUtil.nonNull(mate.getSrcIp());
    }

    private void validBase(SystemCtx sysCtx) {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
        RequireUtil.nonNull(sysCtx.getTimerSvs());
    }

    @Override
    public void checkReplicator(long nodeId) {
        final RaftNodeMate mate = opts.getSelf();
        if (mate.getRole() != RaftNodeStatus.LEADER) {return;}
        //TODO
    }

    private void resetLeaderId(long leaderId) {
        RaftNodeMate self = opts.getSelf();
        long oldLeaderId = self.getLeaderId();
        FsmCallback fsm = opts.getFsmCallback();
        self.setLeaderId(leaderId);
        if (fsm == null) {return;}
        if (oldLeaderId > 0 && leaderId < 0) {
            fsm.onStopFollowing(oldLeaderId, self.getCurTerm());
        } else if (oldLeaderId < 0 && leaderId > 0) {
            fsm.onStartFollowing(leaderId, self.getCurTerm());
        }
    }

}
