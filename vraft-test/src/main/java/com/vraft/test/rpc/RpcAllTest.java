package com.vraft.test.rpc;

import java.util.concurrent.CountDownLatch;

import com.vraft.core.config.ConfigHolder;
import com.vraft.core.rpc.RpcClientImpl;
import com.vraft.core.rpc.RpcManagerImpl;
import com.vraft.core.rpc.RpcServerImpl;
import com.vraft.core.uid.UidHolder;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.config.RpcServerCfg;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.rpc.RpcServer;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jweihsz
 * @version 2024/2/23 19:56
 **/
public class RpcAllTest {
    private final static Logger logger = LogManager.getLogger(RpcAllTest.class);

    private SystemCtx sysCtx;
    private RpcManager rpcManager;
    private ConfigServer configServer;

    @Before
    public void bf() throws Exception {
        sysCtx = new SystemCtx();

        configServer = new ConfigHolder(sysCtx);
        configServer.startup();
        sysCtx.setCfgSvs(configServer);

        UidService uidService = new UidHolder();
        sysCtx.setUidSvs(uidService);

        rpcManager = new RpcManagerImpl(sysCtx);
        rpcManager.startup();
        sysCtx.setRpcMgr(rpcManager);
    }

    @Test
    public void testRpcConnectServerInit() throws Exception {
        CountDownLatch ct = new CountDownLatch(1);
        ConfigServer cfg = sysCtx.getCfgSvs();

        final RpcServerCfg node = cfg.getRpcServerCfg();

        RpcServer rpcServer = new RpcServerImpl(sysCtx, node);
        rpcServer.startup();

        RpcClient rpcClient = new RpcClientImpl(sysCtx, cfg.getRpcClientCfg());
        rpcClient.startup();

        final String serverIp = String.join(":",
            node.getRpcSrvHost(), String.valueOf(node.getRpcSrvPort()));
        long userId = rpcClient.doConnect(serverIp);
        logger.info("client connect {} {}", serverIp, userId);
        ct.await();
    }
}
