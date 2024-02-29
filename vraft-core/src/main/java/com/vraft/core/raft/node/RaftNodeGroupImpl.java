package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftNodeBase;
import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/2/26 15:59
 **/
public class RaftNodeGroupImpl implements RaftNodeGroup {
    private final Map<Long, RaftNodeBase> maps;

    public RaftNodeGroupImpl() {
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public boolean newGroup(long groupId, long nodeId) {
        RaftNodeBase n = null, old = null;
        RaftNodeBase b = maps.get(groupId);
        if (b != null) {return true;}
        n = new RaftNodeBaseImpl(groupId, nodeId);
        old = maps.putIfAbsent(groupId, n);
        return old == null;
    }

    @Override
    public boolean add(long groupId, long nodeId, RaftNodeMate mate) {
        RaftNodeBase b = maps.get(groupId);
        if (b == null) {return false;}
        return b.addPeer(nodeId, mate) == null;
    }

    @Override
    public RaftNodeMate remove(long groupId, long nodeId) {
        RaftNodeBase b = maps.get(groupId);
        if (b == null) {return null;}
        return b.removePeer(nodeId);
    }

    @Override
    public RaftNodeMate get(long groupId, long nodeId) {
        RaftNodeBase b = maps.get(groupId);
        return b == null ? null : b.getPeer(nodeId);
    }

    @Override
    public Map<Long, RaftNodeBase> getAll() {return maps;}

}
