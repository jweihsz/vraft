package com.vraft.facade.raft.elect;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 14:56
 **/
@Data
public class RaftVoteResp {
    private int code;
    private long term;
    private long index;
    private long epoch;
    private long srcTerm;
    private long srcIndex;
    private boolean granted;
}
