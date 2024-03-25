package com.vraft.core.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/25 14:06
 **/
public class RaftNodeMgrImpl implements RaftNodeMgr {
    private final static Logger logger = LogManager.getLogger(RaftNodeMgrImpl.class);

    private final SystemCtx sysCtx;
    private final Map<Long, Map<Long, RaftNode>> group;

    public RaftNodeMgrImpl(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.group = new ConcurrentHashMap<>();
    }

    @Override
    public boolean registerNode(RaftNode node) {
        RaftNodeMate mate = node.getOpts().getSelf();
        if (mate == null) {return false;}
        if (mate.getNodeId() <= 0 || mate.getGroupId() <= 0) {return false;}
        group.computeIfAbsent(mate.getGroupId(), k -> new ConcurrentHashMap<>());
        group.get(mate.getGroupId()).put(mate.getNodeId(), node);
        return true;
    }

    @Override
    public RaftNode unregisterNode(long groupId, long nodeId) {
        Map<Long, RaftNode> maps = group.get(groupId);
        if (maps == null || maps.isEmpty()) {return null;}
        return maps.remove(nodeId);
    }

    @Override
    public RaftNode getNodeMate(long groupId, long nodeId) {
        Map<Long, RaftNode> maps = group.get(groupId);
        if (maps == null || maps.isEmpty()) {return null;}
        return maps.get(nodeId);
    }

}
