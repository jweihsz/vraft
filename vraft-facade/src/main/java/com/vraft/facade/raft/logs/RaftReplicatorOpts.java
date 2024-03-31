package com.vraft.facade.raft.logs;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/31 15:34
 **/
@Data
public class RaftReplicatorOpts {
    private long term;
}
