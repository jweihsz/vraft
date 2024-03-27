package com.vraft.facade.raft.node;

import java.util.concurrent.atomic.AtomicLong;

import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.peers.RaftPeersService;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 11:17
 **/
@Data
public class RaftNodeOpts {
    private RaftNodeMate self;
    private AtomicLong epoch;
    private FsmCallback fsmCallback;
    private int electTimeout = 1000;
    private int maxElectTimeout = 2000;
    private RaftPeersService raftPeers;
    private RaftElectService raftElect;
}
