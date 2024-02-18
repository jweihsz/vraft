package com.vraft.core.rpc;

import com.vraft.facade.rpc.RpcService;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 2024/2/5 14:53
 */
public class RpcHolder implements RpcService {
    private final static Logger logger = LogManager.getLogger(RpcHolder.class);

    private final SystemCtx sysCtx;

    public RpcHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void startup() throws Exception {}

    @Override
    public void shutdown() {}

}
