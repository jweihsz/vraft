package com.vraft.core.raft.peers;

import java.util.LinkedList;

import com.vraft.facade.raft.peers.PeersEntry;

/**
 * @author jweihsz
 * @version 2024/3/20 20:41
 **/
public class PeersConfigMgr {

    private final PeersEntry curCfg;
    private final PeersEntry snapshot;
    private LinkedList<PeersEntry> configurations;

    public PeersConfigMgr() {
        this.curCfg = new PeersEntry();
        this.snapshot = new PeersEntry();
        this.configurations = new LinkedList<>();
    }
}
