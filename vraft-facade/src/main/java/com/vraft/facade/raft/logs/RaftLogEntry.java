package com.vraft.facade.raft.logs;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/4/7 15:55
 **/
@Data
public class RaftLogEntry {
    private byte type;
    private long index;
    private long term;
    private byte[] payload;
}
