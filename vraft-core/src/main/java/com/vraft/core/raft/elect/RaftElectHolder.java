package com.vraft.core.raft.elect;

import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/26 15:12
 **/
public class RaftElectHolder implements RaftElectService {
    private final static Logger logger = LogManager.getLogger(RaftElectHolder.class);

    private final SystemCtx sysCtx;

    public RaftElectHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

}
