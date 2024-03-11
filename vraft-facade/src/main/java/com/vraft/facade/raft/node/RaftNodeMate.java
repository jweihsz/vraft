package com.vraft.facade.raft.node;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 15:45
 **/
@Data
public class RaftNodeMate {
    private int role;
    private long groupId;
    private long nodeId;
    private String srcIp;

    public RaftNodeMate() {}

    public RaftNodeMate(long groupId, long nodeId) {
        this.groupId = groupId;
        this.nodeId = nodeId;
    }
    
}
