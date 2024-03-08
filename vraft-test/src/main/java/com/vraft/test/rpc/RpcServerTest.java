package com.vraft.test.rpc;

import com.vraft.core.config.ConfigHolder;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

/**
 * @author jweihsz
 * @version 2024/2/23 18:23
 **/
public class RpcServerTest {
    private final static Logger logger = LogManager.getLogger(RpcServerTest.class);

    private SystemCtx sysCtx;
    private ConfigServer configServer;

    @Before
    public void bf() throws Exception {
        sysCtx = new SystemCtx();
        configServer = new ConfigHolder(sysCtx);
        configServer.startup();
        sysCtx.setCfgSvs(configServer);
    }

}
