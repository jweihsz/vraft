package com.vraft.core.raft.node;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vraft.core.raft.elect.RaftElectBallot;
import com.vraft.core.utils.OtherUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeOpts;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/3/11 11:33
 **/
public class RaftNodeImpl implements RaftNode {

    private final SystemCtx sysCtx;
    private final RaftNodeOpts opts;
    private final RaftElectBallot ballot;

    private static final ThreadLocal<RaftVoteReq> voteReq;
    private static final ThreadLocal<RaftVoteResp> voteResp;

    static {
        voteReq = new ThreadLocal<>();
        voteResp = new ThreadLocal<>();
    }

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeOpts opts) {
        this.opts = opts;
        this.sysCtx = sysCtx;
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
        if (!canDoPreVote()) {return;}
    }

    private boolean canDoPreVote() {
        return isPreVoteRole()
            && isActive(opts)
            && !isLeaderValid();
    }

    private boolean isPreVoteRole() {
        final long nodeId = opts.getCurNodeId();
        RaftNodeMate mate = opts.getNp().get(nodeId);
        if (mate == null) {return false;}
        final RaftNodeStatus role = mate.getRole();
        return (role == RaftNodeStatus.FOLLOWER);
    }

    private boolean isActive(RaftNodeOpts opts) {
        final long nodeId = opts.getCurNodeId();
        return opts.getNp().containsKey(nodeId)
            || opts.getOp().containsKey(nodeId);
    }

    private boolean isLeaderValid() {
        final long last = opts.getLastLeaderHeat();
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

    private void sendVoteReq(RaftVoteReq req) throws Exception {
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RpcClient client = sysCtx.getRpcClient();
        String uid = RaftVoteReq.class.getName();
        byte[] body = sz.serialize(req);
        Set<Long> filters = null;
        if (!opts.getOp().isEmpty()) {filters = new HashSet<>();}
        for (Map.Entry<Long, RaftNodeMate> entry : opts.getNp().entrySet()) {
            if (entry.getKey() == opts.getCurNodeId()) {continue;}
            final RaftNodeMate mate = entry.getValue();
            long userId = client.doConnect(mate.getSrcIp());
            if (userId < 0) {return;}
            client.oneWay(userId, (byte)0, uid, null, body);
            if (filters != null) {filters.add(entry.getKey());}
        }
        for (Map.Entry<Long, RaftNodeMate> entry : opts.getOp().entrySet()) {
            if (entry.getKey() == opts.getCurNodeId()) {continue;}
            if (filters == null || filters.contains(entry.getKey())) {continue;}
            final RaftNodeMate mate = entry.getValue();
            long userId = client.doConnect(mate.getSrcIp());
            if (userId < 0) {return;}
            client.oneWay(userId, (byte)0, uid, null, body);
        }

    }

    private RaftVoteReq buildVoteReq(RaftNodeMate self) {
        RaftVoteReq req = null;
        req = getVoteReqObj();
        req.setCurTerm(0L);
        req.setLastLogId(0L);
        req.setLastTerm(0L);
        req.setNodeId(self.getNodeId());
        req.setGroupId(self.getGroupId());
        req.setSrcIp(self.getSrcIp());
        return req;
    }

    private RaftVoteReq getVoteReqObj() {
        RaftVoteReq req = voteReq.get();
        if (req != null) {return req;}
        req = new RaftVoteReq();
        voteReq.set(req);
        return req;
    }

    private RaftVoteResp getVoteRespObj() {
        RaftVoteResp resp = voteResp.get();
        if (resp != null) {return resp;}
        resp = new RaftVoteResp();
        voteResp.set(resp);
        return resp;
    }

}
