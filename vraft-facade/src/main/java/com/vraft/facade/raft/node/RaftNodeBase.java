package com.vraft.facade.raft.node;

import java.util.Map;

/**
 * @author jweihsz
 * @version 2024/2/29 13:59
 **/
public interface RaftNodeBase {

    RaftNodeMate getSelf();

    void setLeaderId(long leaderId);

    RaftNodeMate getPeer(long nodeId);

    Map<Long, RaftNodeMate> getPeers();

    RaftNodeMate removePeer(long nodeId);

    boolean isSelf(long groupId, long nodeId);

    RaftNodeMate addPeer(long nodeId, RaftNodeMate mate);

    boolean addPeerIfAbsent(long nodeId, RaftNodeMate mate);

}
