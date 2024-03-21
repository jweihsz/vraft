package com.vraft.facade.raft.peers;

import java.util.LinkedList;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/20 21:18
 **/
public interface PeersService extends LifeCycle {

    default PeersEntry getCurCfg() {return null;}

    default PeersEntry getSnapshot() {return null;}

    default LinkedList<PeersEntry> getLists() {return null;}
}
