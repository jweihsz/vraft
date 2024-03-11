package com.vraft.core.raft.node;

import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;

/**
 * @author jweihsz
 * @version 2024/3/11 11:33
 **/
public class RaftNodeImpl implements RaftNode {

    private RaftNodeMate mate;

    public RaftNodeImpl(RaftNodeMate mate) {
        this.mate = mate;
    }
}
