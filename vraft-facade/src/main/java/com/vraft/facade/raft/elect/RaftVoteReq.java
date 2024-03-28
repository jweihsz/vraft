package com.vraft.facade.raft.elect;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 14:55
 **/
@Data
public class RaftVoteReq {
    private boolean isPre;
    private long groupId;
    private long nodeId;
    private long curTerm;
    private long epoch;
    private String srcIp;
    private long lastLogTerm;
    private long lastLogIndex;
}

