package com.vraft.facade.raft.node;

import com.vraft.facade.raft.fsm.FsmCallback;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 11:17
 **/
@Data
public class RaftNodeOpts {
    private RaftNodeMate mate;
    private int electTimeout = 1000;
    private FsmCallback fsmCallback;
    private volatile long leaderId = -1L;
    private volatile long lastLeaderHeat;
    private volatile long curTerm = 0L;
}
