package com.vraft.facade.raft.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.facade.raft.fsm.FsmCallback;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 11:17
 **/
@Data
public class RaftNodeOpts {

    private int electTimeout = 1000;
    private FsmCallback fsmCallback;
    private volatile long curTerm = 0L;
    private volatile long lastLeaderHeat;
    private volatile long curNodeId = -1L;
    private volatile long leaderId = -1L;

    private final Map<Long, RaftNodeMate> np;
    private final Map<Long, RaftNodeMate> op;

    public RaftNodeOpts() {
        this.np = new ConcurrentHashMap<>();
        this.op = new ConcurrentHashMap<>();
    }

}
