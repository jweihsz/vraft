package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.node.RaftGroup;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeOpts;
import com.vraft.facade.rpc.RpcServer;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public class RaftGroupImpl implements RaftGroup {

    private RaftNodeOpts opts;
    private final SystemCtx sysCtx;
    private final AtomicBoolean start;
    private final Map<Long, RaftNode> peers;
    private long leaderId, selfId, groupId;

    public RaftGroupImpl(SystemCtx sysCtx, RaftNodeOpts nOpts) {
        this.sysCtx = sysCtx;
        this.opts = nOpts;
        this.peers = new ConcurrentHashMap<>();
        this.start = new AtomicBoolean(false);
    }

    @Override
    public void init() throws Exception {
        validRpcSrv(sysCtx);
        validNodeOpts(opts);
        this.peers.clear();
        final RaftNodeMate mate = opts.getMate();
        RaftNode node = new RaftNodeImpl(sysCtx, mate);
        this.selfId = mate.getNodeId();
        this.groupId = mate.getGroupId();
        this.peers.put(selfId, node);
    }

    @Override
    public void startup() throws Exception {
        if (!start.compareAndSet(false, true)) {return;}
        final RpcServer rpcServer = sysCtx.getRpcSrv();
        rpcServer.startup();
    }

    @Override
    public void shutdown() {}

    @Override
    public void setLeaderId(long leaderId) {
        this.leaderId = leaderId;
    }

    @Override
    public RaftNode removePeer(long nodeId) {
        return peers.remove(nodeId);
    }

    @Override
    public RaftNode getPeer(long nodeId) {
        return peers.get(nodeId);
    }

    @Override
    public boolean isSelf(long groupId, long nodeId) {
        return groupId == this.groupId
            && nodeId == this.selfId;
    }

    @Override
    public boolean addPeerIfAbsent(long nodeId, RaftNode mate) {
        return peers.putIfAbsent(nodeId, mate) == null;
    }

    @Override
    public RaftNode addPeer(long nodeId, RaftNode mate) {
        return peers.put(nodeId, mate);
    }

    @Override
    public RaftNode getSelf() {return peers.get(selfId);}

    @Override
    public Map<Long, RaftNode> getPeers() {return peers;}

    private void validNodeOpts(RaftNodeOpts opts) {
        RequireUtil.nonNull(opts);
        validNodeMate(opts.getMate());
    }

    private void validNodeMate(RaftNodeMate mate) {
        RequireUtil.nonNull(mate);
        RequireUtil.nonNull(mate.getSrcIp());
        RequireUtil.isTrue(mate.getGroupId() > 0);
        RequireUtil.isTrue(mate.getNodeId() > 0);
    }

    private void validRpcSrv(SystemCtx sysCtx) {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
    }
}
