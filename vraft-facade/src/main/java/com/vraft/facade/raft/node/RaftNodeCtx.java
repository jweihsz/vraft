package com.vraft.facade.raft.node;

import java.util.concurrent.atomic.AtomicLong;

import com.vraft.facade.raft.elect.RaftElectMgr;
import com.vraft.facade.raft.fsm.FsmCallback;
import com.vraft.facade.raft.logs.RaftLogsMgr;
import com.vraft.facade.raft.peers.RaftPeersMgr;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 11:17
 **/
@Data
public class RaftNodeCtx {
    private RaftNodeMate self;
    private AtomicLong epoch;
    private int electTimeout;
    private int maxElectTimeout;
    private RaftLogsMgr logsMgr;
    private RaftPeersMgr peersMgr;
    private RaftElectMgr electMgr;
    private FsmCallback fsmCallback;
}
