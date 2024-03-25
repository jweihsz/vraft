package com.vraft.core.raft.logs;

import com.vraft.facade.raft.logs.LogsMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/25 16:16
 **/
public class LogsMgrImpl implements LogsMgr {
    private final static Logger logger = LogManager.getLogger(LogsMgrImpl.class);

    private final SystemCtx sysCtx;

    public LogsMgrImpl(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }
}
