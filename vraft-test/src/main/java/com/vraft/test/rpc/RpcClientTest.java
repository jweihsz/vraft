package com.vraft.test.rpc;

import java.util.concurrent.CountDownLatch;

import com.vraft.core.config.ConfigHolder;
import com.vraft.core.rpc.RpcClientImpl;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jweihsz
 * @version 2024/2/23 19:47
 **/
public class RpcClientTest {
    private final static Logger logger = LogManager.getLogger(RpcClientTest.class);

    private SystemCtx sysCtx;
    private ConfigServer configServer;

    @Before
    public void bf() throws Exception {
        sysCtx = new SystemCtx();
        configServer = new ConfigHolder(sysCtx);
        configServer.startup();
        sysCtx.setCfgSvs(configServer);
    }

    @Test
    public void testRpcClientInit() throws Exception {
        CountDownLatch ct = new CountDownLatch(1);
        ConfigServer cfg = sysCtx.getCfgSvs();
        RpcClient rpcClient = new RpcClientImpl(sysCtx, cfg.getRpcClientCfg());
        rpcClient.startup();
        ct.await();
    }
}
