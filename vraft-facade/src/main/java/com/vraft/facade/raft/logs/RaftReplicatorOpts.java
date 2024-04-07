package com.vraft.facade.raft.logs;

import com.vraft.facade.raft.node.RaftNode;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/31 15:34
 **/
@Data
public class RaftReplicatorOpts {
    private RaftNode node;
    private long term;
    private long nextSendLogIndex;
    private long lastRpcSendTimestamp;
    private long dynamicHeartBeatTimeoutMs;
}
