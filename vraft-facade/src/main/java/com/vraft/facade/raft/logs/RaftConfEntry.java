package com.vraft.facade.raft.logs;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/4/12 21:07
 **/
@Data
public class RaftConfEntry {
    private byte type;
    private long index;
    private long term;
    private long groupId;
    private byte[] payload;

    public RaftConfEntry() {}

    public RaftConfEntry(long groupId, long term,
        long index, byte type, byte[] payload) {
        this.groupId = groupId;
        this.term = term;
        this.index = index;
        this.type = type;
        this.payload = payload;
    }
}
