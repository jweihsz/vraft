package com.vraft.facade.raft.node;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/11 11:31
 **/
public interface RaftNode extends LifeCycle {

    RaftNodeOpts getOpts();

    void checkReplicator(long nodeId);
}
