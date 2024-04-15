package com.vraft.facade.raft.logs;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/4/6 17:28
 **/
public interface RaftLogStore extends LifeCycle {

    byte[] getLastLog(long groupId);

    byte[] getFirstLog(long groupId);

    RaftLogEntry getEntry(long groupId, long term, long index) throws Exception;

    RaftConfEntry getConf(long groupId, long term, long index) throws Exception;
}
