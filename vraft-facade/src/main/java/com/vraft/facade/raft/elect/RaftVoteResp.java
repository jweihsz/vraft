package com.vraft.facade.raft.elect;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/26 14:56
 **/
@Data
public class RaftVoteResp {
    private long term;
    private boolean granted;
    private int code;
}
