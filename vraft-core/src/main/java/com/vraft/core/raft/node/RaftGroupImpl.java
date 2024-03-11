package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftGroup;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.rpc.RpcServer;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public class RaftGroupImpl implements RaftGroup {
    private final long groupId;
    private long leaderId, selfId;
    private final RpcServer rpcServer;
    private final Map<Long, RaftNode> peers;

    public RaftGroupImpl(long groupId,
        RpcServer rpcServer) {
        this.groupId = groupId;
        this.rpcServer = rpcServer;
        this.peers = new ConcurrentHashMap<>();
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {
        this.rpcServer.startup();
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
    public void addSelf(long nodeId) {
        this.selfId = nodeId;
        RaftNodeMate mate = new RaftNodeMate(groupId, nodeId);
        this.peers.put(nodeId, new RaftNodeImpl(mate));
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
}
