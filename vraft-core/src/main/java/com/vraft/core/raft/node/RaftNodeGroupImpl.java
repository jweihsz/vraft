package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public class RaftNodeGroupImpl implements RaftNodeGroup {

    private final long groupId;
    private long leaderId, selfId;
    private final Map<Long, RaftNodeMate> peers;

    public RaftNodeGroupImpl(long groupId) {
        this.groupId = groupId;
        this.peers = new ConcurrentHashMap<>();
    }

    @Override
    public void setLeaderId(long leaderId) {
        this.leaderId = leaderId;
    }

    @Override
    public RaftNodeMate removePeer(long nodeId) {
        return peers.remove(nodeId);
    }

    @Override
    public RaftNodeMate getPeer(long nodeId) {
        return peers.get(nodeId);
    }

    @Override
    public boolean isSelf(long groupId, long nodeId) {
        return groupId == this.groupId
            && nodeId == this.selfId;
    }

    @Override
    public boolean addPeerIfAbsent(
        long nodeId, RaftNodeMate mate) {
        return peers.putIfAbsent(nodeId, mate) == null;
    }

    @Override
    public RaftNodeMate addPeer(
        long nodeId, RaftNodeMate mate) {
        return peers.put(nodeId, mate);
    }

    @Override
    public void addSelf(long nodeId) {
        this.selfId = nodeId;
        this.peers.put(nodeId, new RaftNodeMate(groupId, nodeId));
    }

    @Override
    public RaftNodeMate getSelf() {return peers.get(selfId);}

    @Override
    public Map<Long, RaftNodeMate> getPeers() {return peers;}
}
