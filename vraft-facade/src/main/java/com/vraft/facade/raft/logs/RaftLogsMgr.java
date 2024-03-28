package com.vraft.facade.raft.logs;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/3/25 16:17
 **/
public interface RaftLogsMgr extends LifeCycle {

    long getLastLogIndex();

    void setLastLogIndex(long lastLogIndex);

    long getLastLogTerm();

    void setLastLogTerm(long lastLogTerm);

    RaftVoteMate getVoteMate();

    void setVoteMate(long term, long nodeId);
}
