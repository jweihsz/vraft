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
import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeCmd;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.raft.node.RaftNodeOpts;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.raft.peers.PeersEntry;
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
public class RaftElectHolder implements RaftElectService {
    private final static Logger logger = LogManager.getLogger(RaftElectHolder.class);

    private final RaftNode node;
    private final SystemCtx sysCtx;
    private final Consumer<Object> apply;
    private final RaftElectBallot ballot;

    public RaftElectHolder(SystemCtx sysCtx,
        RaftNode node) {
        this.node = node;
        this.sysCtx = sysCtx;
        this.apply = buildFuncApply();
        this.ballot = new RaftElectBallot(sysCtx);
    }

    @Override
    public void startVote(Boolean isPre) {
        final RaftNodeOpts opts = node.getOpts();
        TimerService timer = sysCtx.getTimerSvs();
        long delay = OtherUtil.randomTimeout(
            opts.getElectTimeout(),
            opts.getMaxElectTimeout());
        timer.addTimeout(apply, isPre, delay);
    }

    @Override
    public void doVote(boolean isPre) throws Exception {
        final RaftNodeOpts opts = node.getOpts();
        long selfNodeId = opts.getSelf().getNodeId();
        if (!isPreVoteRole()) {return;}
        if (!isInMember(selfNodeId)) {return;}
        if (isLeaderValid()) {return;}
        sendVoteReq(buildVoteReq(isPre));
        startVote(isPre);
    }

    @Override
    public void processPreVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("pre vote resp:{}", resp);
        if (!validPreVoteResp(resp)) {return;}
        if (!doPreVoteGranted(resp.getSrcNodeId())) {return;}
        logger.info("PreVote OK");
    }

    @Override
    public byte[] processPreVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate self = opts.getSelf();
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftVoteResp res = ObjectsPool.getVoteRespObj();
        long nodeId = req.getNodeId();
        long groupId = req.getGroupId();
        res.setEpoch(req.getEpoch());
        res.setTerm(req.getLastTerm());
        res.setIndex(req.getLastIndex());
        res.setSrcTerm(self.getLastTerm());
        res.setSrcIndex(self.getLastIndex());
        res.setGranted(false);
        res.setCode(Code.SUCCESS);
        res.setSrcNodeId(self.getNodeId());
        res.setSrcGroupId(self.getGroupId());
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
            res.setGranted(canGrantedVote(self, req));
        }
        return sz.serialize(res);
    }

    private boolean canGrantedVote(RaftNodeMate self, RaftVoteReq req) {
        if (self.getCurTerm() > req.getLastTerm()) {return false;}
        if (self.getCurTerm() < req.getLastTerm()) {return true;}
        return self.getLastIndex() <= req.getLastIndex();
    }

    private boolean isActive() {
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus status = mate.getRole();
        return status.ordinal() < RaftNodeStatus.ERROR.ordinal();
    }

    private boolean doPreVoteGranted(long nodeId) {
        ballot.doGrant(nodeId);
        return ballot.isGranted();
    }

    private boolean validPreVoteResp(RaftVoteResp resp) {
        if (resp.getCode() != Code.SUCCESS) {return false;}
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate mate = opts.getSelf();
        if (resp.getSrcTerm() > mate.getCurTerm()) {
            doStepDown();
            return false;
        }
        return !((mate.getRole() != RaftNodeStatus.FOLLOWER
            || resp.getTerm() != mate.getCurTerm()
            || resp.getEpoch() != getEpoch()
            || !resp.isGranted()));
    }

    private void doStepDown() {

    }

    private boolean isPreVoteRole() {
        RaftNodeOpts opts = node.getOpts();
        RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus role = mate.getRole();
        return (role == RaftNodeStatus.FOLLOWER);
    }

    private Consumer<Object> buildFuncApply() {
        return (obj) -> {
            ByteBuf bf = null;
            try {
                if (!(obj instanceof Boolean)) {return;}
                final RaftNodeOpts opts = node.getOpts();
                final RaftNodeMate self = opts.getSelf();
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

    private void sendVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate self = opts.getSelf();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RpcClient client = sysCtx.getRpcClient();
        String uid = RaftVoteReq.class.getName();
        byte[] body = sz.serialize(req);
        Set<Long> filters = null;
        PeersEntry e = opts.getRaftPeers().getCurEntry();
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
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate self = opts.getSelf();
        RaftInnerCmd pkg = ObjectsPool.getInnerCmdObj();
        pkg.setCmd(isPre ? RaftNodeCmd.CMD_DO_PRE_VOTE
            : RaftNodeCmd.CMD_DO_FOR_VOTE);
        final String uid = RaftInnerCmd.class.getName();
        return RpcCommon.buildBasePkg(
            RpcConsts.RPC_ONE_WAY, true,
            self.getGroupId(), self.getNodeId(),
            -1L, uid, null, sz.serialize(pkg));
    }

    private boolean isInMember(long nodeId) {
        final RaftNodeOpts opts = node.getOpts();
        PeersEntry entry = opts.getRaftPeers().getCurEntry();
        return entry.getCurConf().hasKey(nodeId)
            || entry.getOldConf().hasKey(nodeId);
    }

    private boolean isLeaderValid() {
        final RaftNodeOpts opts = node.getOpts();
        final long last = opts.getSelf().getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < opts.getElectTimeout();
    }

    private RaftVoteReq buildVoteReq(boolean isPre) {
        final RaftNodeOpts opts = node.getOpts();
        final RaftNodeMate self = opts.getSelf();
        RaftVoteReq req = null;
        req = ObjectsPool.getVoteReqObj();
        req.setPre(isPre);
        req.setEpoch(nextEpoch());
        req.setCurTerm(self.getCurTerm());
        req.setLastIndex(self.getLastIndex());
        req.setLastTerm(self.getLastTerm());
        req.setNodeId(self.getNodeId());
        req.setGroupId(self.getGroupId());
        req.setSrcIp(self.getSrcIp());
        return req;
    }

    private long nextEpoch() {
        final RaftNodeOpts opts = node.getOpts();
        return opts.getEpoch().getAndIncrement();
    }

    private long getEpoch() {
        final RaftNodeOpts opts = node.getOpts();
        return opts.getEpoch().get();
    }

}
