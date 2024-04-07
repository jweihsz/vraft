package com.vraft.core.raft.logs;

import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/4/7 14:22
 **/
public class RaftLogBallotBox {
    private final static Logger logger = LogManager.getLogger(RaftLogBallotBox.class);

    private final RaftNode node;
    private final SystemCtx sysCtx;

    public RaftLogBallotBox(SystemCtx sysCtx, RaftNode node) {
        this.node = node;
        this.sysCtx = sysCtx;
    }
    
}
