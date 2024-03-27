package com.vraft.facade.raft.elect;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/26 21:12
 **/
@Data
public class RaftInnerCmd {
    private int cmd;
    private byte[] payload;
}
