package com.vraft.facade.raft.node;

import java.util.Map;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public interface RaftGroup extends LifeCycle {

    RaftNode getSelf();

    RaftNode getPeer(long nodeId);

    Map<Long, RaftNode> getPeers();

    RaftNode removePeer(long nodeId);

    void setLeaderId(long leaderId);
    
    boolean isSelf(long groupId, long nodeId);

    RaftNode addPeer(long nodeId, RaftNode mate);

    boolean addPeerIfAbsent(long nodeId, RaftNode mate);

}
