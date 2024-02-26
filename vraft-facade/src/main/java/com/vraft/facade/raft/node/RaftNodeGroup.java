package com.vraft.facade.raft.node;

/**
 * @author jweihsz
 * @version 2024/2/26 15:57
 **/
public interface RaftNodeGroup {

    RaftNodeMate get(long groupId, long nodeId);

    RaftNodeMate remove(long groupId, long nodeId);

    void add(long groupId, long nodeId, RaftNodeMate mate);
}
