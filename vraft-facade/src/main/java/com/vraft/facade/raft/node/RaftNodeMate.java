package com.vraft.facade.raft.node;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 15:45
 **/
@Data
public class RaftNodeMate {
    private long nodeId;
    private long groupId;
    private long leaderId;
    private long curTerm;
    private String srcIp;
    private RaftNodeStatus role;
    private long lastLeaderHeat;

    public RaftNodeMate() {}

    public RaftNodeMate(long groupId, long nodeId) {
        this.groupId = groupId;
        this.nodeId = nodeId;
    }

    public RaftNodeMate(long nodeId, String srcIp) {
        this.srcIp = srcIp;
        this.nodeId = nodeId;
    }

}
