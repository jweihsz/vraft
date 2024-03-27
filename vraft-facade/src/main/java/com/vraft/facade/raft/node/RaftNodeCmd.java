package com.vraft.facade.raft.node;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/26 21:15
 **/
@Data
public class RaftNodeCmd {
    private RaftNodeCmd() {}

    public static int CMD_DO_PRE_VOTE = 0x01;
    public static int CMD_DO_FOR_VOTE = 0x02;
}
