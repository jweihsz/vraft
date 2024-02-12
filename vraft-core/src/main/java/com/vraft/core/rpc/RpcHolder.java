package com.vraft.core.rpc;

import com.vraft.facade.rpc.RpcConsts;
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

    private RpcServer serverInstance() {
        RpcBuilder b = raftServerBuilder();
        return new RpcServer(b);
    }

    private RpcClient clientInstance() {
        RpcBuilder b = raftClientBuilder();
        return new RpcClient(b);
    }

    private RpcBuilder raftServerBuilder() {
        RpcBuilder b = new RpcBuilder();
        b.setBossNum(1).setWorkerNum(8);
        b.setType(RpcConsts.SERVER).setWire(RpcConsts.TCP);
        return b;
    }

    private RpcBuilder raftClientBuilder() {
        RpcBuilder b = new RpcBuilder();
        b.setBossNum(4);
        b.setType(RpcConsts.CLIENT).setWire(RpcConsts.TCP);
        return b;
    }

}
