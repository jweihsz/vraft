package com.vraft.facade.raft.elect;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 14:56
 **/
@Data
public class RaftVoteResp {
    private int code;
    private long epoch;
    private long term;
    private boolean isPre;
    private boolean granted;
    private long respTerm;
    private long respNodeId;
    private long respGroupId;
    private long reqLastLogTerm;
    private long reqLastLogIndex;
    private long respLastLogTerm;
    private long respLastLogIndex;
}
