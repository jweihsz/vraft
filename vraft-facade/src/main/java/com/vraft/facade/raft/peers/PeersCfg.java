package com.vraft.facade.raft.peers;

import java.util.LinkedList;
import java.util.List;

import com.vraft.facade.raft.node.RaftNodeMate;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/21 10:23
 **/
@Data
public class PeersCfg {
    private List<RaftNodeMate> peers;
    private List<RaftNodeMate> learners;

    public PeersCfg() {
        this.peers = new LinkedList<>();
        this.learners = new LinkedList<>();
    }
}
