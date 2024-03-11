package com.vraft.facade.raft.node;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof RaftNodeMate)) {return false;}
        RaftNodeMate mate = (RaftNodeMate)o;
        return mate.groupId == this.groupId
            && mate.nodeId == this.getNodeId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, nodeId);
    }

}
