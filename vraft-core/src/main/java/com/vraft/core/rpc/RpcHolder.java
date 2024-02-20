package com.vraft.core.rpc;

import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/20 17:19
 **/
public class RpcHolder {
    private final static Logger logger = LogManager.getLogger(RpcHolder.class);

    private final SystemCtx sysCtx;
    
    public RpcHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

}
