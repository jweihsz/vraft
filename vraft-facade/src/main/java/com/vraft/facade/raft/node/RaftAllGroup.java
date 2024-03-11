package com.vraft.facade.raft.node;

import java.util.Map;

/**
 * @author jweihsz
 * @version 2024/2/26 15:57
 **/
public interface RaftAllGroup {

    boolean newGroup(long groupId);
    
    Map<Long, RaftNodeGroup> getAll();

    RaftNodeMate get(long groupId, long nodeId);

    RaftNodeMate remove(long groupId, long nodeId);

    boolean add(long groupId, long nodeId, RaftNodeMate mate);

}
