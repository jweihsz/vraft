package com.vraft.core.raft.elect;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.core.rpc.RpcCommon;
import com.vraft.core.utils.OtherUtil;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.common.Code;
import com.vraft.facade.raft.elect.RaftElectMgr;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.logs.RaftLogsMgr;
import com.vraft.facade.raft.logs.RaftVoteFor;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeCmd;
import com.vraft.facade.raft.node.RaftNodeCtx;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.raft.peers.PeersEntry;
import com.vraft.facade.raft.peers.RaftPeersMgr;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/26 21:35
 **/
public class RaftElectMgrImpl implements RaftElectMgr {
    private final static Logger logger = LogManager.getLogger(RaftElectMgrImpl.class);

    private final RaftNode node;
    private final SystemCtx sysCtx;
    private final Consumer<Object> apply;
    private final RaftElectBallot ballot;

    public RaftElectMgrImpl(SystemCtx sysCtx,
        RaftNode node) {
        this.node = node;
        this.sysCtx = sysCtx;
        this.apply = buildFuncApply();
        this.ballot = new RaftElectBallot(sysCtx);
    }

    @Override
    public void init() throws Exception { }

    @Override
    public void startVote(Boolean isPre) {
        final RaftNodeCtx ctx = node.getNodeCtx();
        TimerService timer = sysCtx.getTimerSvs();
        long delay = OtherUtil.randomTimeout(
            ctx.getElectTimeout(),
            ctx.getMaxElectTimeout());
        timer.addTimeout(apply, isPre, delay);
    }

    @Override
    public void doForVote() throws Exception {
        final RaftNodeCtx ctx = node.getNodeCtx();
        RaftLogsMgr logsMgr = ctx.getLogsMgr();
        RaftPeersMgr raftPeers = ctx.getPeersMgr();
        final RaftNodeMate self = ctx.getSelf();
        final long selfNodeId = self.getNodeId();
        if (self.getRole() != RaftNodeStatus.CANDIDATE) {return;}
        if (!isInMember(selfNodeId)) {return;}
        ballot.init(raftPeers.getCurEntry(), true);
        ballot.doGrant(ctx.getSelf().getNodeId());
        logsMgr.setVoteMate(self.getCurTerm(), selfNodeId);
        sendVoteReq(buildVoteReq(false));
        startVote(false);
    }

    @Override
    public void doPreVote() throws Exception {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate self = ctx.getSelf();
        RaftPeersMgr raftPeers = ctx.getPeersMgr();
        final long selfNodeId = self.getNodeId();
        if (self.getRole() != RaftNodeStatus.FOLLOWER) {return;}
        if (!isInMember(selfNodeId)) {return;}
        if (isLeaderValid()) {return;}
        ballot.init(raftPeers.getCurEntry(), true);
        ballot.doGrant(ctx.getSelf().getNodeId());
        sendVoteReq(buildVoteReq(true));
        startVote(true);
    }

    @Override
    public void processPreVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("pre vote resp:{}", resp);
        if (resp.getCode() != Code.SUCCESS) {return;}
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftLogsMgr logsMgr = ctx.getLogsMgr();
        final RaftNodeMate mate = ctx.getSelf();
        if (resp.getRespLastLogTerm() > logsMgr.getLastLogTerm()) {
            // doStepDown();
            return;
        }
        if (mate.getRole() != RaftNodeStatus.FOLLOWER) {
            return;
        }
        if (resp.getTerm() != mate.getCurTerm() + 1
            || resp.getEpoch() != getEpoch()) {
            return;
        }
        if (!resp.isGranted()) {return;}
        ballot.doGrant(resp.getRespNodeId());
        if (!ballot.isGranted()) {return;}
        nextEpoch();
        electSelf();
    }

    @Override
    public void processForVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("for vote resp:{}", resp);
        if (resp.getCode() != Code.SUCCESS) {return;}
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftLogsMgr logsMgr = ctx.getLogsMgr();
        final RaftNodeMate mate = ctx.getSelf();
        if (resp.getRespLastLogTerm() > logsMgr.getLastLogTerm()) {
            // doStepDown(resp.getTerm());
            return;
        }
        if (mate.getRole() != RaftNodeStatus.CANDIDATE) {
            return;
        }
        if (resp.getTerm() != mate.getCurTerm()
            || resp.getEpoch() != getEpoch()) {
            return;
        }
        if (!resp.isGranted()) {return;}
        ballot.doGrant(resp.getRespNodeId());
        if (!ballot.isGranted()) {return;}
        nextEpoch();
        mate.setRole(RaftNodeStatus.LEARNER);
        logger.info("OK!");

    }

    @Override
    public byte[] processForVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate self = ctx.getSelf();
        final RaftLogsMgr logsMgr = ctx.getLogsMgr();
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftVoteResp res = ObjectsPool.getVoteRespObj();
        long nodeId = self.getNodeId();
        long groupId = self.getGroupId();
        res.setPre(false);
        res.setGranted(false);
        res.setTerm(req.getCurTerm());
        res.setCode(Code.SUCCESS);
        res.setEpoch(req.getEpoch());
        res.setRespNodeId(nodeId);
        res.setRespGroupId(groupId);
        res.setReqLastLogTerm(req.getLastLogTerm());
        res.setReqLastLogIndex(req.getLastLogIndex());
        res.setRespLastLogTerm(logsMgr.getLastLogTerm());
        res.setRespLastLogIndex(logsMgr.getLastLogIndex());
        RaftNode node = mgr.getNodeMate(groupId, nodeId);
        if (node == null || !isActive()) {
            res.setCode(Code.RAFT_NOT_ACTIVE);
            return sz.serialize(res);
        }
        if (!isInMember(req.getNodeId())) {
            res.setCode(Code.RAFT_NOT_MEMBER);
            return sz.serialize(res);
        }
        if (req.getCurTerm() < self.getCurTerm()) {
            res.setCode(Code.RAFT_TERM_SMALLER);
            return sz.serialize(res);
        }
        //if (req.getCurTerm() == self.getCurTerm()
        //    && self.getRole().ordinal() <=
        //    RaftNodeStatus.CANDIDATE.ordinal()) {
        //    res.setCode(Code.RAFT_VALID_ROLE);
        //    return sz.serialize(res);
        //}
        if (req.getCurTerm() > self.getCurTerm()) {
            doStepDown(req.getCurTerm());
        }
        RaftVoteFor mate = logsMgr.getVoteMate();
        if (mate.getNodeId() <= 0 && canGrantedVote(req)) {
            logsMgr.setVoteMate(req.getCurTerm(), req.getNodeId());
        }
        if (req.getCurTerm() == self.getCurTerm()
            && mate.getNodeId() == req.getNodeId()) {
            res.setGranted(true);
        }
        return sz.serialize(res);
    }

    @Override
    public byte[] processPreVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate self = ctx.getSelf();
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        final RaftLogsMgr logsMgr = ctx.getLogsMgr();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftVoteResp res = ObjectsPool.getVoteRespObj();
        res.setPre(true);
        res.setTerm(req.getCurTerm());
        res.setEpoch(req.getEpoch());
        res.setReqLastLogTerm(req.getLastLogTerm());
        res.setReqLastLogIndex(req.getLastLogIndex());
        res.setRespLastLogTerm(logsMgr.getLastLogTerm());
        res.setRespLastLogIndex(logsMgr.getLastLogIndex());
        res.setGranted(false);
        res.setCode(Code.SUCCESS);
        res.setRespNodeId(self.getNodeId());
        res.setRespGroupId(self.getGroupId());
        RaftNode node = mgr.getNodeMate(
            self.getGroupId(), self.getNodeId());
        if (node == null || !isActive()) {
            res.setCode(Code.RAFT_NOT_ACTIVE);
            return sz.serialize(res);
        } else if (!isInMember(req.getNodeId())) {
            res.setCode(Code.RAFT_NOT_MEMBER);
        } else if (isLeaderValid()) {
            res.setCode(Code.RAFT_VALID_LEADER);
        } else if (req.getCurTerm() < self.getCurTerm()) {
            res.setCode(Code.RAFT_TERM_SMALLER);
            node.checkReplicator(req.getNodeId());
        } else {
            node.checkReplicator(req.getNodeId());
            res.setGranted(canGrantedVote(req));
        }
        return sz.serialize(res);
    }

    private boolean canGrantedVote(RaftVoteReq req) {
        RaftLogsMgr logsMgr = node.getNodeCtx().getLogsMgr();
        if (logsMgr.getLastLogTerm() > req.getLastLogTerm()) {return false;}
        if (logsMgr.getLastLogTerm() < req.getLastLogTerm()) {return true;}
        return logsMgr.getLastLogIndex() <= req.getLastLogIndex();
    }

    private boolean isActive() {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate mate = ctx.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus status = mate.getRole();
        return status.ordinal() < RaftNodeStatus.ERROR.ordinal();
    }

    private void doStepDown(long term) {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate mate = ctx.getSelf();
        if (term > mate.getCurTerm()) {
            mate.setCurTerm(term);
            // voteNodeId = -1L;
            // voteTerm = -1L;
        }
    }

    private Consumer<Object> buildFuncApply() {
        return (obj) -> {
            ByteBuf bf = null;
            try {
                if (!(obj instanceof Boolean)) {return;}
                final RaftNodeCtx ctx = node.getNodeCtx();
                final RaftNodeMate self = ctx.getSelf();
                final boolean isPre = (Boolean)obj;
                ActorService actorSrv = sysCtx.getActorSvs();
                bf = buildVoteCmdPkg(isPre);
                final long groupId = self.getGroupId();
                final long nodeId = self.getNodeId();
                actorSrv.dispatchRaftGroup(groupId, nodeId, bf.retain());
            } catch (Exception ex) {
                ex.printStackTrace();
                if (bf == null) {return;}
                ReferenceCountUtil.safeRelease(bf);
            }
        };
    }

    private void electSelf() {
        RaftNodeCtx ctx = node.getNodeCtx();
        RaftNodeMate self = ctx.getSelf();
        if (!isActive()) {return;}
        if (!isInMember(self.getNodeId())) {return;}
        self.setRole(RaftNodeStatus.CANDIDATE);
        self.setCurTerm(self.getCurTerm() + 1);
        startVote(false);
    }

    private void sendVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate self = ctx.getSelf();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RpcClient client = sysCtx.getRpcClient();
        String uid = RaftVoteReq.class.getName();
        byte[] body = sz.serialize(req);
        Set<Long> filters = null;
        PeersEntry e = ctx.getPeersMgr().getCurEntry();
        if (!e.getOldConf().isEmpty()) {filters = new HashSet<>();}
        Map<Long, RaftNodeMate> peers = e.getCurConf().getPeers();
        for (Map.Entry<Long, RaftNodeMate> entry : peers.entrySet()) {
            if (entry.getKey() == self.getNodeId()) {continue;}
            final RaftNodeMate mate = entry.getValue();
            long userId = client.doConnect(mate.getSrcIp());
            if (userId < 0) {continue;}
            client.oneWay(userId, -1L, (byte)0, self.getGroupId(),
                mate.getNodeId(), uid, null, body);
            if (filters != null) {filters.add(entry.getKey());}
        }
        Map<Long, RaftNodeMate> old = e.getOldConf().getPeers();
        for (Map.Entry<Long, RaftNodeMate> entry : old.entrySet()) {
            if (entry.getKey() == self.getNodeId()) {continue;}
            if (filters == null || filters.contains(entry.getKey())) {continue;}
            final RaftNodeMate mate = entry.getValue();
            long userId = client.doConnect(mate.getSrcIp());
            if (userId < 0) {continue;}
            client.oneWay(userId, -1L, (byte)0, self.getGroupId(),
                mate.getNodeId(), uid, null, body);
        }
    }

    private ByteBuf buildVoteCmdPkg(boolean isPre) throws Exception {
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        final RaftNodeCtx ctx = node.getNodeCtx();
        final RaftNodeMate self = ctx.getSelf();
        final String uid = RaftInnerCmd.class.getName();
        RaftInnerCmd pkg = ObjectsPool.getInnerCmdObj();
        if (isPre) {
            pkg.setCmd(RaftNodeCmd.CMD_DO_PRE_VOTE);
        } else {
            pkg.setCmd(RaftNodeCmd.CMD_DO_FOR_VOTE);
        }
        return RpcCommon.buildBasePkg(
            RpcConsts.RPC_ONE_WAY, true,
            self.getGroupId(), self.getNodeId(),
            -1L, uid, null, sz.serialize(pkg));
    }

    private boolean isInMember(long nodeId) {
        RaftNodeCtx ctx = node.getNodeCtx();
        PeersEntry entry = ctx.getPeersMgr().getCurEntry();
        return entry.getCurConf().hasKey(nodeId)
            || entry.getOldConf().hasKey(nodeId);
    }

    private boolean isLeaderValid() {
        RaftNodeCtx ctx = node.getNodeCtx();
        final long last = ctx.getSelf().getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < ctx.getElectTimeout();
    }

    private RaftVoteReq buildVoteReq(boolean isPre) {
        RaftNodeCtx ctx = node.getNodeCtx();
        RaftLogsMgr logsMgr = ctx.getLogsMgr();
        final RaftNodeMate self = ctx.getSelf();
        RaftVoteReq req = null;
        req = ObjectsPool.getVoteReqObj();
        req.setPre(isPre);
        req.setEpoch(nextEpoch());
        req.setCurTerm(isPre
            ? self.getCurTerm() + 1
            : self.getCurTerm());
        req.setLastLogIndex(logsMgr.getLastLogIndex());
        req.setLastLogTerm(logsMgr.getLastLogTerm());
        req.setNodeId(self.getNodeId());
        req.setGroupId(self.getGroupId());
        req.setSrcIp(self.getSrcIp());
        return req;
    }

    private long nextEpoch() {
        RaftNodeCtx ctx = node.getNodeCtx();
        return ctx.getEpoch().incrementAndGet();
    }

    private long getEpoch() {
        RaftNodeCtx ctx = node.getNodeCtx();
        return ctx.getEpoch().get();
    }

}
