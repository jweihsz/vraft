package com.vraft.facade.raft.node;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 11:17
 **/
@Data
public class RaftNodeOpts {
    private RaftNodeMate mate;
}
