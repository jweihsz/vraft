package com.vraft.facade.raft.peers;

import com.vraft.facade.raft.node.RaftNodeMate;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/20 20:39
 **/
@Data
public class PeersEntry {
    private PeersCfg curConf;
    private PeersCfg oldConf;

    public static PeersEntry build() {
        PeersEntry res = new PeersEntry();
        res.setCurConf(new PeersCfg());
        res.setOldConf(new PeersCfg());
        return res;
    }

    public RaftNodeMate getNodeFromPeer(long nodeId) {
        RaftNodeMate mate = curConf.getPeers().get(nodeId);
        if (mate != null) {return mate;}
        return oldConf.getPeers().get(nodeId);
    }

    public RaftNodeMate getNodeFromLeaner(long nodeId) {
        RaftNodeMate mate = curConf.getLearners().get(nodeId);
        if (mate != null) {return mate;}
        return oldConf.getLearners().get(nodeId);
    }

    public RaftNodeMate getNode(long nodeId) {
        RaftNodeMate mate = getNodeFromPeer(nodeId);
        return mate != null ? mate : getNodeFromLeaner(nodeId);
    }
}
