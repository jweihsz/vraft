package com.vraft.core.raft.logs;

import com.vraft.facade.raft.logs.RaftLogsMgr;
import com.vraft.facade.raft.logs.RaftVoteFor;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/25 16:16
 **/
public class RaftLogMgrImpl implements RaftLogsMgr {
    private final static Logger logger = LogManager.getLogger(RaftLogMgrImpl.class);

    private final SystemCtx sysCtx;
    private volatile long lastLogTerm;
    private volatile long lastLogIndex;
    private final RaftVoteFor voteMate;

    public RaftLogMgrImpl(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.lastLogTerm = 0L;
        this.lastLogIndex = 0L;
        this.voteMate = new RaftVoteFor();
    }

    @Override
    public void init() throws Exception {
        this.voteMate.setTerm(-1L);
        this.voteMate.setNodeId(-1L);
    }

    @Override
    public long getLastLogIndex() {
        return lastLogIndex;
    }

    @Override
    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    @Override
    public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public RaftVoteFor getVoteMate() {
        return voteMate;
    }

    @Override
    public void setVoteMate(long term, long nodeId) {
        this.voteMate.setTerm(term);
        this.voteMate.setNodeId(nodeId);
    }

}
