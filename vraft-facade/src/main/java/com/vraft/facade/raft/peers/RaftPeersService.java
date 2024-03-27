package com.vraft.facade.raft.peers;

import java.util.LinkedList;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/20 21:18
 **/
public interface RaftPeersService extends LifeCycle {

    default PeersEntry getCurEntry() {return null;}

    default PeersEntry getSnapshotEntry() {return null;}

    default LinkedList<PeersEntry> getHisEntry() {return null;}
}
