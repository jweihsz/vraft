package com.vraft.core.raft.node;

import java.util.ArrayList;
import java.util.List;

import com.vraft.core.raft.elect.RaftElectBallot;
import com.vraft.core.utils.OtherUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeOpts;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/3/11 11:33
 **/
public class RaftNodeImpl implements RaftNode {

    private final SystemCtx sysCtx;
    private final RaftNodeOpts opts;
    private final List<Long> curPeers;
    private final List<Long> oldPeers;
    private final RaftElectBallot ballot;

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeOpts opts) {
        this.opts = opts;
        this.sysCtx = sysCtx;
        this.oldPeers = new ArrayList<>();
        this.curPeers = new ArrayList<>();
        this.ballot = new RaftElectBallot(sysCtx);
    }

    @Override
    public void shutdown() {}

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {
        valid(sysCtx);
    }

    private void valid(SystemCtx sysCtx) {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
        RequireUtil.nonNull(sysCtx.getTimerSvs());
    }

    private void doPreVote() {
        final RaftNodeMate mate = opts.getMate();
        final RaftNodeStatus role = mate.getRole();
        if (role != RaftNodeStatus.FOLLOWER) {return;}
        if (!isActive(mate)) {return;}

    }

    private boolean isActive(RaftNodeMate mate) {
        final long nodeId = mate.getNodeId();
        return curPeers.contains(nodeId)
            || oldPeers.contains(nodeId);
    }

    private boolean isLeaderValid() {
        long last = opts.getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < opts.getElectTimeout();
    }

    private void resetLeaderId(long leaderId) {
        long oldLeaderId = opts.getLeaderId();
        FsmCallback fsm = opts.getFsmCallback();
        opts.setLeaderId(leaderId);
        if (fsm == null) {return;}
        if (oldLeaderId > 0 && leaderId < 0) {
            fsm.onStopFollowing(oldLeaderId, opts.getCurTerm());
        } else if (oldLeaderId < 0 && leaderId > 0) {
            fsm.onStartFollowing(leaderId, opts.getCurTerm());
        }
    }

}
