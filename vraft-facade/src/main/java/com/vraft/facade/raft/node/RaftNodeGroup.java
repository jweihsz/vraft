package com.vraft.facade.raft.node;

import java.util.Map;
import java.util.Set;

/**
 * @author jweihsz
 * @version 2024/2/26 15:57
 **/
public interface RaftNodeGroup {

    Map<Long, Set<RaftNodeMate>> getAll();

    RaftNodeMate get(long groupId, long nodeId);

    RaftNodeMate remove(long groupId, long nodeId);

    void add(long groupId, long nodeId, RaftNodeMate mate);

}
