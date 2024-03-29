package com.vraft.facade.raft.peers;

import java.util.LinkedList;
import java.util.Set;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/20 21:18
 **/
public interface RaftPeersMgr extends LifeCycle {

    default Set<Long> getAllNodeIds() {return null;}
    
    default PeersEntry getCurEntry() {return null;}

    default PeersEntry getSnapshotEntry() {return null;}

    default boolean isLearner(long nodeId) {return false;}

    default LinkedList<PeersEntry> getHisEntry() {return null;}

}
