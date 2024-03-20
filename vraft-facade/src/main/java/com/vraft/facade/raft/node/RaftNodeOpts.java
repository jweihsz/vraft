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
    private RaftNodeMate self;
    private FsmCallback fsmCallback;
    private int electTimeout = 1000;
    private int maxElectTimeout = 2000;
    private final Map<Long, RaftNodeMate> np;
    private final Map<Long, RaftNodeMate> op;

    public RaftNodeOpts() {
        this.np = new ConcurrentHashMap<>();
        this.op = new ConcurrentHashMap<>();
    }

}
