package com.vraft.facade.raft.node;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 15:45
 **/
@Data
public class RaftNodeMate {
    private long groupId;
    private long nodeId;
    private String srcIp;
    private long lastTerm;
    private long lastLogId;
    private long leaderId;
    private long curTerm;
    private long lastLeaderHeat;
    private RaftNodeStatus role;

    public RaftNodeMate() {}

    public RaftNodeMate(long groupId, long nodeId) {
        this.groupId = groupId;
        this.nodeId = nodeId;
    }

}
