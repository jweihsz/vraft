package com.vraft.core.raft.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import com.vraft.core.raft.elect.RaftElectBallot;
import com.vraft.core.raft.peers.PeersManager;
import com.vraft.core.utils.OtherUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
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

        PeersService peersSrv = new PeersManager(sysCtx);
        peersSrv.init();
        opts.setPeersService(peersSrv);

        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        sz.registerClz(Arrays.asList(RaftVoteReq.class, RaftVoteResp.class));

    }

    @Override
    public void startup() throws Exception {
        startVote(true);
    }

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
        } else if (!isPre && forVoteTask != null) {
            timer.removeTimeout(forVoteTask);
            forVoteTask = null;
        }
    }

    private void startVote(boolean isPre) {
        Consumer<Object> apply = null;
        TimerService timer = sysCtx.getTimerSvs();
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

    private boolean canDoPreVote() {
        return isPreVoteRole()
            && isActive(opts)
            && !isLeaderValid();
    }

    private boolean isPreVoteRole() {
        RaftNodeMate mate = opts.getSelf();
        if (mate == null) {return false;}
        final RaftNodeStatus role = mate.getRole();
        return (role == RaftNodeStatus.FOLLOWER);
    }

    private boolean isActive(RaftNodeOpts opts) {
        long nodeId = opts.getSelf().getNodeId();
        PeersService peersSrv = opts.getPeersService();
        PeersEntry entry = peersSrv.getCurEntry();
        return entry.getCurConf().hasKey(nodeId)
            || entry.getOldConf().hasKey(nodeId);
    }

    private boolean isLeaderValid() {
        final long last = opts.getSelf().getLastLeaderHeat();
        return OtherUtil.getSysMs() - last < opts.getElectTimeout();
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
            client.oneWay(userId, (byte)0, uid, null, body);
            if (filters != null) {filters.add(entry.getKey());}
        }
        Map<Long, RaftNodeMate> old = e.getOldConf().getPeers();
        for (Map.Entry<Long, RaftNodeMate> entry : old.entrySet()) {
            if (entry.getKey() == self.getNodeId()) {continue;}
            if (filters == null || filters.contains(entry.getKey())) {continue;}
            final RaftNodeMate mate = entry.getValue();
            long userId = client.doConnect(mate.getSrcIp());
            if (userId < 0) {continue;}
            client.oneWay(userId, (byte)0, uid, null, body);
        }

    }

    private RaftVoteReq buildVoteReq(boolean isPre) {
        final RaftNodeMate self = opts.getSelf();
        RaftVoteReq req = null;
        req = getVoteReqObj();
        req.setPre(isPre);
        req.setCurTerm(self.getCurTerm());
        req.setLastLogId(self.getLastLogId());
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
