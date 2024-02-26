package com.vraft.core.raft;

import com.vraft.core.raft.node.RaftNodeGroupImpl;
import com.vraft.facade.raft.RaftService;
import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/26 16:27
 **/
public class RaftHolder implements RaftService {
    private final static Logger logger = LogManager.getLogger(RaftHolder.class);

    private final SystemCtx sysCtx;
    private final RaftNodeGroup nodeGroup;

    private RaftHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.nodeGroup = new RaftNodeGroupImpl();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void startup() throws Exception { }

}
