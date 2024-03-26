package com.vraft.core.raft.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.vraft.core.raft.elect.RaftElectBallot;
import com.vraft.core.raft.peers.PeersMgrImpl;
import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.OtherUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.common.Code;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.raft.node.RaftNodeOpts;
import com.vraft.facade.raft.node.RaftNodeStatus;
import com.vraft.facade.raft.peers.PeersEntry;
import com.vraft.facade.raft.peers.PeersService;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
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
    private final RaftElectBallot ballot;
    private Object preVoteTask, forVoteTask;
    private final AtomicBoolean preVoteStatus;
    private final AtomicBoolean forVoteStatus;
    private final Consumer<Object> preVoteApply;
    private final Consumer<Object> forVoteApply;
    private static final ThreadLocal<RaftVoteReq> voteReq;
    private static final ThreadLocal<RaftVoteResp> voteResp;

    static {
        voteReq = new ThreadLocal<>();
        voteResp = new ThreadLocal<>();
    }

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeMate self) {
        this.sysCtx = sysCtx;
        this.opts = new RaftNodeOpts(self);
        this.ballot = new RaftElectBallot(sysCtx);
        this.preVoteApply = (p) -> doVote(true);
        this.forVoteApply = (p) -> doVote(false);
        this.preVoteStatus = new AtomicBoolean(false);
        this.forVoteStatus = new AtomicBoolean(false);
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

        PeersService peersSrv = new PeersMgrImpl(sysCtx);
        peersSrv.init();
        opts.setPeersService(peersSrv);

        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        sz.registerClz(Arrays.asList(RaftVoteReq.class, RaftVoteResp.class));

        sysCtx.getRaftNodeMgr().registerNode(this);
    }

    @Override
    public void startup() throws Exception {
        startVote(true, true);

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

    private void stopVote(boolean isPre) {
        TimerService timer = sysCtx.getTimerSvs();
        if (isPre && preVoteTask != null) {
            timer.removeTimeout(preVoteTask);
            preVoteTask = null;
            preVoteStatus.set(false);
        } else if (!isPre && forVoteTask != null) {
            timer.removeTimeout(forVoteTask);
            forVoteTask = null;
            forVoteStatus.set(false);
        }
    }

    private void startVote(boolean isPre, boolean force) {
        Consumer<Object> apply = null;
        TimerService timer = sysCtx.getTimerSvs();
        if (!force && isPre && !preVoteStatus.get()) {return;}
        if (!force && !isPre && !forVoteStatus.get()) {return;}
        if (force && isPre) {preVoteStatus.set(true);}
        if (force && !isPre) {forVoteStatus.set(true);}
        apply = isPre ? preVoteApply : forVoteApply;
        long delay = randomTimeout(opts.getElectTimeout());
        Object task = timer.addTimeout(apply, sysCtx, delay);
        if (isPre) {preVoteTask = task;} else {forVoteTask = task;}
    }

    private void doVote(boolean isPre) {
        if (isPre) {
            doPreVote();
        } else {
            doForVote();
        }
        startVote(isPre, false);
    }

    private void doForVote() {
        try {
            if (!canDoForPre()) {return;}
        } catch (Exception ex) {ex.printStackTrace();}
    }

    private void doPreVote() {
        try {
            if (!canDoPreVote()) {return;}
            sendVoteReq(buildVoteReq(false));
        } catch (Exception ex) {ex.printStackTrace();}
    }

    private boolean canDoForPre() {
        RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus role = mate.getRole();
        return (role == RaftNodeStatus.CANDIDATE);
    }

    private boolean isActive() {
        RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus status = mate.getRole();
        return status.ordinal() < RaftNodeStatus.ERROR.ordinal();
    }

    private boolean canDoPreVote() {
        long selfNodeId = opts.getSelf().getNodeId();
        return isPreVoteRole() && isInMember(selfNodeId)
            && !isLeaderValid();
    }

    private boolean isPreVoteRole() {
        RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus role = mate.getRole();
        return (role == RaftNodeStatus.FOLLOWER);
    }

    private boolean isInMember(long nodeId) {
        PeersService peersSrv = opts.getPeersService();
        PeersEntry entry = peersSrv.getCurEntry();
        return entry.getCurConf().hasKey(nodeId)
            || entry.getOldConf().hasKey(nodeId);
    }

    private boolean isLeaderValid() {
        final long last = opts.getSelf().getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < opts.getElectTimeout();
    }

    private void checkReplicator(long nodeId) {
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

    private void sendVoteReq(RaftVoteReq req) throws Exception {
        final RaftNodeMate self = opts.getSelf();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RpcClient client = sysCtx.getRpcClient();
        String uid = RaftVoteReq.class.getName();
        byte[] body = sz.serialize(req);
        Set<Long> filters = null;
        PeersService peersSrv = opts.getPeersService();
        PeersEntry e = peersSrv.getCurEntry();
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

    @Override
    public void processPreVoteResp(RaftVoteResp resp) throws Exception {
        logger.info("pre vote resp:{}", resp);
        if (!validPreVoteResp(resp)) {return;}
        if (!doPreVoteGranted(resp.getSrcNodeId())) {return;}
    }

    private boolean validPreVoteResp(RaftVoteResp resp) {
        if (resp.getCode() != Code.SUCCESS) {return false;}
        final RaftNodeMate mate = opts.getSelf();
        if (resp.getSrcTerm() > mate.getCurTerm()) {
            doStepDown();
            return false;
        }
        if (mate.getRole() != RaftNodeStatus.FOLLOWER) {
            return false;
        }
        if (resp.getTerm() != mate.getCurTerm()) {
            return false;
        }
        if (resp.getEpoch() != getEpoch()) {
            return false;
        }
        return resp.isGranted();
    }

    private boolean doPreVoteGranted(long nodeId) {
        ballot.doGrant(nodeId);
        return ballot.isGranted();
    }

    @Override
    public byte[] processPreVoteReq(RaftVoteReq req) throws Exception {
        RaftVoteResp res = getVoteRespObj();
        final RaftNodeMate self = opts.getSelf();
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
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
        RaftNode node = mgr.getNodeMate(groupId, nodeId);
        if (node == null || !isActive()) {
            res.setCode(Code.RAFT_NOT_ACTIVE);
            return sz.serialize(res);
        } else if (!isInMember(req.getNodeId())) {
            res.setCode(Code.RAFT_NOT_MEMBER);
        } else if (isLeaderValid()) {
            res.setCode(Code.RAFT_VALID_LEADER);
        } else if (req.getCurTerm() < self.getCurTerm()) {
            res.setCode(Code.RAFT_TERM_SMALLER);
            checkReplicator(req.getNodeId());
        } else {
            checkReplicator(req.getNodeId());
            res.setGranted(canGrantedVote(self, req));
        }
        return sz.serialize(res);
    }

    private void doStepDown() {

    }

    private boolean canGrantedVote(RaftNodeMate self, RaftVoteReq req) {
        if (self.getCurTerm() > req.getLastTerm()) {return false;}
        if (self.getCurTerm() < req.getLastTerm()) {return true;}
        return self.getLastIndex() <= req.getLastIndex();
    }

    private long nextEpoch() {
        return opts.getEpoch().getAndIncrement();
    }

    private long getEpoch() {
        return opts.getEpoch().get();
    }

    private RaftVoteReq buildVoteReq(boolean isPre) {
        final RaftNodeMate self = opts.getSelf();
        RaftVoteReq req = null;
        req = getVoteReqObj();
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

    private int randomTimeout(int timeoutMs) {
        return ThreadLocalRandom.current()
            .nextInt(timeoutMs, opts.getMaxElectTimeout());
    }

}
