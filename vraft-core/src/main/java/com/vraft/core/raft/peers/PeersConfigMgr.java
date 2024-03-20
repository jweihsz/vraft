package com.vraft.core.raft.peers;

import java.util.LinkedList;

import com.vraft.facade.raft.peers.PeersCfgEntry;

/**
 * @author jweihsz
 * @version 2024/3/20 20:41
 **/
public class PeersConfigMgr {

    private final PeersCfgEntry curCfg;
    private final PeersCfgEntry snapshot;
    private LinkedList<PeersCfgEntry> configurations;

    public PeersConfigMgr() {
        this.curCfg = new PeersCfgEntry();
        this.snapshot = new PeersCfgEntry();
        this.configurations = new LinkedList<>();
    }
}
