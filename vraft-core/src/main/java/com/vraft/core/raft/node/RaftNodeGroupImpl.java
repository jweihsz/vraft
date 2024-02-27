package com.vraft.core.raft.node;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/2/26 15:59
 **/
public class RaftNodeGroupImpl implements RaftNodeGroup {
    private final Map<Long, Set<RaftNodeMate>> maps;

    public RaftNodeGroupImpl() {
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public void add(long groupId, long nodeId, RaftNodeMate mate) {
        Set<RaftNodeMate> n = null, old = null;
        Set<RaftNodeMate> b = maps.get(groupId);
        if (b == null) {
            n = new CopyOnWriteArraySet<>();
            old = maps.put(groupId, n);
            b = old != null ? old : n;
        }
        b.add(mate);
    }

    @Override
    public RaftNodeMate remove(long groupId, long nodeId) {
        Set<RaftNodeMate> b = maps.get(groupId);
        if (b == null) { return null;}
        RaftNodeMate node = null;
        Iterator<RaftNodeMate> it = b.iterator();
        while (it.hasNext()) {
            node = it.next();
            if (node.getNodeId() == nodeId
                && node.getGroupId() == groupId) {
                it.remove();
                return node;
            }
        }
        return null;
    }

    @Override
    public RaftNodeMate get(long groupId, long nodeId) {
        Set<RaftNodeMate> b = maps.get(groupId);
        if (b == null) { return null;}
        RaftNodeMate node = null;
        for (RaftNodeMate raftNodeMate : b) {
            node = raftNodeMate;
            if (node.getNodeId() == nodeId
                && node.getGroupId() == groupId) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Map<Long, Set<RaftNodeMate>> getAll() {return maps;}

}
