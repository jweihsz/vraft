package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftNodeBase;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public class RaftNodeBaseImpl implements RaftNodeBase {

    private final long groupId, nodeId;
    private final Map<Long, RaftNodeMate> peers;

    public RaftNodeBaseImpl(long groupId, long nodeId) {
        this.groupId = groupId;
        this.nodeId = nodeId;
        this.peers = new ConcurrentHashMap<>();
        this.peers.put(nodeId, new RaftNodeMate(groupId, nodeId));
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
            && nodeId == this.nodeId;
    }

    @Override
    public boolean addPeerIfAbsent(long nodeId, RaftNodeMate mate) {
        return peers.putIfAbsent(nodeId, mate) == null;
    }

    @Override
    public RaftNodeMate addPeer(long nodeId, RaftNodeMate mate) {
        return peers.put(nodeId, mate);
    }

    @Override
    public RaftNodeMate getSelf() {return peers.get(nodeId);}

    @Override
    public Map<Long, RaftNodeMate> getPeers() {return peers;}
}
