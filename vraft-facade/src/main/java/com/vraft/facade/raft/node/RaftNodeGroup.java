package com.vraft.facade.raft.node;

import java.util.Map;

/**
 * @author jweihsz
 * @version 2024/2/26 15:57
 **/
public interface RaftNodeGroup {

    Map<Long, RaftNodeBase> getAll();

    boolean newGroup(long groupId, long nodeId);

    RaftNodeMate get(long groupId, long nodeId);

    RaftNodeMate remove(long groupId, long nodeId);

    boolean add(long groupId, long nodeId, RaftNodeMate mate);

}
