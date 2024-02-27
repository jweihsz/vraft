package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/2/26 15:59
 **/
public class RaftNodeGroupImpl implements RaftNodeGroup {
    private final Map<Long, Map<Long, RaftNodeMate>> maps;

    public RaftNodeGroupImpl() {
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public void add(long groupId, long nodeId, RaftNodeMate mate) {
        Map<Long, RaftNodeMate> b = maps.get(groupId), old = null;
        if (b == null) {
            old = maps.put(groupId, new ConcurrentHashMap<>());
            b = old != null ? old : maps.get(groupId);
        }
        b.put(nodeId, mate);
    }

    @Override
    public RaftNodeMate remove(long groupId, long nodeId) {
        Map<Long, RaftNodeMate> b = maps.get(groupId);
        if (b == null) { return null;}
        RaftNodeMate res = b.remove(nodeId);
        if (b.isEmpty()) {maps.remove(groupId);}
        return res;
    }

    @Override
    public RaftNodeMate get(long groupId, long nodeId) {
        Map<Long, RaftNodeMate> b = maps.get(groupId);
        if (b == null) { return null;}
        return b.getOrDefault(nodeId, null);
    }

    @Override
    public Map<Long, Map<Long, RaftNodeMate>> getAll() {return maps;}

}
