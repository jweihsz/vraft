package com.vraft.facade.raft.peers;

import java.util.HashMap;
import java.util.Map;

import com.vraft.facade.raft.node.RaftNodeMate;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/21 10:23
 **/
@Data
public class PeersCfg {
    private Map<Long, RaftNodeMate> peers;
    private Map<Long, RaftNodeMate> learners;

    public PeersCfg() {
        this.peers = new HashMap<>();
        this.learners = new HashMap<>();
    }

    public boolean hasKeyInPeers(long key) {
        return peers.containsKey(key);
    }

    public boolean hasKeyInLearners(long key) {
        return learners.containsKey(key);
    }

    public boolean hasKey(long key) {
        return learners.containsKey(key) || peers.containsKey(key);
    }

    public boolean isEmpty() {
        return learners.isEmpty() && peers.isEmpty();
    }
}
