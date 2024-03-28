package com.vraft.facade.raft.logs;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/28 17:39
 **/
@Data
public class RaftVoteFor {
    private long term;
    private long nodeId;
}
