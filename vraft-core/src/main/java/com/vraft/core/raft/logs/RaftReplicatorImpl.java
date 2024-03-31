package com.vraft.core.raft.logs;

import com.vraft.facade.raft.logs.RaftReplicator;
import com.vraft.facade.raft.logs.RaftReplicatorOpts;
import com.vraft.facade.raft.logs.RaftReplicatorType;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/30 00:07
 **/
public class RaftReplicatorImpl implements RaftReplicator {
    private final static Logger logger = LogManager.getLogger(RaftReplicatorImpl.class);

    private final RaftNode node;
    private final SystemCtx sysCtx;
    private final RaftReplicatorOpts opts;

    public RaftReplicatorImpl(SystemCtx sysCtx, RaftNode node) {
        this.node = node;
        this.sysCtx = sysCtx;
        this.opts = new RaftReplicatorOpts();
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {}

    @Override
    public void shutdown() {}

    private RaftReplicatorOpts newOpts(RaftNode node) {
        return new RaftReplicatorOpts();
    }

    @Override
    public boolean resetTerm(final long newTerm) {
        if (newTerm <= this.opts.getTerm()) {return false;}
        this.opts.setTerm(newTerm);
        return true;
    }

    @Override
    public boolean addReplicator(long nodeId,
        RaftReplicatorType type, boolean sync) {

        return true;
    }

}
