package com.vraft.core.raft.logs;

import com.vraft.facade.raft.logs.RaftReplicator;
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

    public RaftReplicatorImpl(SystemCtx sysCtx, RaftNode node) {
        this.node = node;
        this.sysCtx = sysCtx;
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {}

    @Override
    public void shutdown() {}

}
