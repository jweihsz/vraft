package com.vraft.facade.raft.node;

import java.util.Map;

import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.peers.PeersService;
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
    private Map<Long, RaftNodeMate> np;
    private Map<Long, RaftNodeMate> op;
    private PeersService peersService;
}
