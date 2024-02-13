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
    private final RpcServer rpcRaftServer;
    private final RpcClient rpcRaftClient;

    public RpcHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.rpcRaftClient = buildRpcRaftClient();
        this.rpcRaftServer = buildRpcRaftServer();
    }

    @Override
    public void startup() throws Exception {
        this.rpcRaftClient.startup();
        this.rpcRaftServer.startup();
    }

    @Override
    public void shutdown() {
        this.rpcRaftClient.shutdown();
        this.rpcRaftServer.shutdown();
    }

    private RpcServer buildRpcRaftServer() {

        return null;
    }

    private RpcClient buildRpcRaftClient() {

        return null;
    }

}
