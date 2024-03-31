package com.vraft.facade.raft.logs;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/30 00:06
 **/
public interface RaftReplicator extends LifeCycle {

    boolean resetTerm(final long newTerm);

    boolean addReplicator(long nodeId, RaftReplicatorType type, boolean sync);
}
