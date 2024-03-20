package com.vraft.facade.raft.peers;

import java.util.ArrayList;
import java.util.List;

import com.vraft.facade.raft.node.RaftNodeMate;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/20 20:37
 **/
@Data
public class PeersCfgNode {
    private List<RaftNodeMate> peers;
    private List<RaftNodeMate> learners;

    public PeersCfgNode() {
        this.peers = new ArrayList<>();
        this.learners = new ArrayList<>();
    }

}
