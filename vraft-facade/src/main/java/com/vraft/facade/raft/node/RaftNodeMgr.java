package com.vraft.facade.raft.node;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/25 14:06
 **/
public interface RaftNodeMgr extends LifeCycle {

    boolean registerNode(RaftNode node);

    RaftNode getNodeMate(long groupId, long nodeId);

    RaftNode unregisterNode(long groupId, long nodeId);
    
}
