package com.vraft.core.raft.elect;

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
import com.vraft.facade.raft.fsm.FsmCallback;
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
    public void doStepDown(long term) {
        if (!isActive()) {return;}
        RaftNodeCtx ctx = node.getNodeCtx();
        RaftLogsMgr logsMgr = ctx.getLogsMgr();
        RaftPeersMgr peersMgr = ctx.getPeersMgr();
        RaftNodeMate self = ctx.getSelf();
        self.setRole(RaftNodeStatus.FOLLOWER);
        self.setLastLeaderHeat(OtherUtil.getSysMs());
        if (term > self.getCurTerm()) {
            self.setCurTerm(term);
            logsMgr.setVoteMate(term, -1L);
        }
        if (!peersMgr.isLearner(self.getNodeId())) {
            sendVoteReqPkg(true);
        }
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
        startVoteTimer(false);
    }

    @Override
    public void doPreVote() throws Exception {
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        RaftPeersMgr raftPeers = nodeCtx.getPeersMgr();
        final long selfNodeId = self.getNodeId();
        if (self.getRole() != RaftNodeStatus.FOLLOWER) {return;}
        if (!isInMember(selfNodeId)) {return;}
        if (isLeaderValid()) {return;}
        resetLeaderId(-1L);
        ballot.init(raftPeers.getCurEntry(), true);
        ballot.doGrant(nodeCtx.getSelf().getNodeId());
        sendVoteReq(buildVoteReq(true));
        startVoteTimer(true);
    }

    @Override
    public void processPreVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("pre vote resp:{}", resp);
        if (resp.getCode() != Code.SUCCESS) {return;}
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        RaftNodeMate self = nodeCtx.getSelf();
        if (resp.getEpoch() != getEpoch()) {return;}
        if (self.getRole() != RaftNodeStatus.FOLLOWER) {return;}
        if (resp.getTerm() != self.getCurTerm() + 1) {return;}
        if (resp.getRespTerm() > self.getCurTerm()) {
            doStepDown(resp.getRespTerm());
            return;
        }
        if (!resp.isGranted()) {return;}
        ballot.doGrant(resp.getRespNodeId());
        if (ballot.isGranted()) { electSelf();}
    }

    @Override
    public void processForVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("for vote resp:{}", resp);
        if (resp.getCode() != Code.SUCCESS) {return;}
        final RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        if (self.getRole() != RaftNodeStatus.CANDIDATE) {return;}
        if (resp.getEpoch() != getEpoch()) {return;}
        if (resp.getTerm() != self.getCurTerm()) {return;}
        if (resp.getRespTerm() > self.getCurTerm()) {
            doStepDown(resp.getRespTerm());
            return;
        }
        if (!resp.isGranted()) {return;}
        ballot.doGrant(resp.getRespNodeId());
        if (!ballot.isGranted()) {return;}
        nextEpoch();
        self.setRole(RaftNodeStatus.LEADER);
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
        res.setRespTerm(self.getCurTerm());
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

        if (req.getCurTerm() > self.getCurTerm()) {
            doStepDown(req.getCurTerm());
        }
        RaftVoteFor voteFor = logsMgr.getVoteMate();
        if (voteFor.getNodeId() <= 0 && canGrantedVote(req)) {
            doStepDown(req.getCurTerm());
            logsMgr.setVoteMate(req.getCurTerm(), req.getNodeId());
        }
        if (req.getCurTerm() == self.getCurTerm()
            && voteFor.getNodeId() == req.getNodeId()) {
            res.setGranted(true);
        }
        return sz.serialize(res);
    }

    @Override
    public byte[] processPreVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        final RaftLogsMgr logsMgr = nodeCtx.getLogsMgr();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftVoteResp res = ObjectsPool.getVoteRespObj();
        res.setPre(true);
        res.setGranted(false);
        res.setCode(Code.SUCCESS);
        res.setTerm(req.getCurTerm());
        res.setEpoch(req.getEpoch());
        res.setRespTerm(self.getCurTerm());
        res.setRespNodeId(self.getNodeId());
        res.setRespGroupId(self.getGroupId());
        res.setReqLastLogTerm(req.getLastLogTerm());
        res.setReqLastLogIndex(req.getLastLogIndex());
        res.setRespLastLogTerm(logsMgr.getLastLogTerm());
        res.setRespLastLogIndex(logsMgr.getLastLogIndex());
        RaftNode node = mgr.getNodeMate(
            self.getGroupId(), self.getNodeId());
        if (node == null || !isActive()) {
            res.setCode(Code.RAFT_NOT_ACTIVE);
            return sz.serialize(res);
        }
        if (!isInMember(req.getNodeId())) {
            res.setCode(Code.RAFT_NOT_MEMBER);
            return sz.serialize(res);
        }
        if (isLeader() || isLeaderValid()) {
            res.setCode(Code.RAFT_VALID_LEADER);
            return sz.serialize(res);
        }
        if (req.getCurTerm() < self.getCurTerm()) {
            res.setCode(Code.RAFT_TERM_SMALLER);
            node.checkReplicator(req.getNodeId());
            return sz.serialize(res);
        }
        node.checkReplicator(req.getNodeId());
        res.setGranted(canGrantedVote(req));
        return sz.serialize(res);
    }

    private void startVoteTimer(boolean isPre) {
        final RaftNodeCtx ctx = node.getNodeCtx();
        TimerService timer = sysCtx.getTimerSvs();
        long delay = OtherUtil.randomTimeout(
            ctx.getElectTimeout(),
            ctx.getMaxElectTimeout());
        timer.addTimeout(apply, isPre, delay);
    }

    private void resetLeaderId(long leaderId) {
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        RaftNodeMate self = nodeCtx.getSelf();
        long oldLeaderId = self.getLeaderId();
        FsmCallback fsm = nodeCtx.getFsmCallback();
        self.setLeaderId(leaderId);
        if (fsm == null) {return;}
        if (oldLeaderId > 0 && leaderId < 0) {
            fsm.onStopFollowing(oldLeaderId, self.getCurTerm());
        } else if (oldLeaderId < 0 && leaderId > 0) {
            fsm.onStartFollowing(leaderId, self.getCurTerm());
        }
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

    private Consumer<Object> buildFuncApply() {
        final RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        return (obj) -> {
            if (!(obj instanceof Boolean)) {return;}
            final boolean isPre = (Boolean)obj;
            if (!isPre && !isLeader()) {
                doStepDown(self.getCurTerm());
                sendVoteReqPkg(true);
            } else if (isPre && !isLeaderValid()) {
                sendVoteReqPkg(true);
            }
        };
    }

    private boolean sendVoteReqPkg(boolean isPre) {
        ByteBuf bf = null;
        try {
            final RaftNodeCtx nodeCtx = node.getNodeCtx();
            final RaftNodeMate self = nodeCtx.getSelf();
            ActorService actorSrv = sysCtx.getActorSvs();
            bf = buildVoteCmdPkg(isPre);
            final long groupId = self.getGroupId();
            final long nodeId = self.getNodeId();
            return actorSrv.dispatchRaftGroup(groupId, nodeId, bf.retain());
        } catch (Exception ex) {
            ex.printStackTrace();
            if (bf == null) {return false;}
            ReferenceCountUtil.safeRelease(bf);
            return false;
        }
    }

    private void electSelf() {
        nextEpoch();
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        RaftNodeMate self = nodeCtx.getSelf();
        if (!isActive()) {return;}
        if (!isInMember(self.getNodeId())) {return;}
        resetLeaderId(-1L);
        self.setRole(RaftNodeStatus.CANDIDATE);
        self.setCurTerm(self.getCurTerm() + 1);
        sendVoteReqPkg(false);
    }

    private void sendVoteReq(RaftVoteReq req) throws Exception {
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RpcClient client = sysCtx.getRpcClient();
        String uid = RaftVoteReq.class.getName();
        byte[] body = sz.serialize(req);
        RaftPeersMgr peersMgr = nodeCtx.getPeersMgr();
        final PeersEntry e = peersMgr.getCurEntry();
        final Set<Long> nodeIds = peersMgr.getAllNodeIds();
        if (nodeIds == null || nodeIds.isEmpty()) {return;}
        for (Long nodeId : nodeIds) {
            if (nodeId == self.getNodeId()) {continue;}
            RaftNodeMate mate = e.getNodeFromPeer(nodeId);
            if (mate == null) {continue;}
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
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        RaftNodeMate self = nodeCtx.getSelf();
        if (self.getLeaderId() <= 0) {return false;}
        final long last = self.getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < nodeCtx.getElectTimeout();
    }

    private boolean isLeader() {
        final RaftNodeCtx nodeCtx = node.getNodeCtx();
        final RaftNodeMate self = nodeCtx.getSelf();
        return self.getRole() == RaftNodeStatus.LEADER;
    }

    private RaftVoteReq buildVoteReq(boolean isPre) {
        RaftNodeCtx nodeCtx = node.getNodeCtx();
        RaftNodeMate self = nodeCtx.getSelf();
        RaftLogsMgr logsMgr = nodeCtx.getLogsMgr();
        long lastLogTerm = logsMgr.getLastLogTerm();
        long lastLogIndex = logsMgr.getLastLogIndex();
        long curTerm = self.getCurTerm() + (isPre ? 1 : 0);
        RaftVoteReq req = null;
        req = ObjectsPool.getVoteReqObj();
        req.setPre(isPre);
        req.setCurTerm(curTerm);
        req.setEpoch(nextEpoch());
        req.setSrcIp(self.getSrcIp());
        req.setLastLogIndex(lastLogIndex);
        req.setLastLogTerm(lastLogTerm);
        req.setNodeId(self.getNodeId());
        req.setGroupId(self.getGroupId());
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
