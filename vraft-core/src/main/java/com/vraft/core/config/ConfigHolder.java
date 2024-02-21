package com.vraft.core.config;

import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/21 17:42
 **/
public class ConfigHolder implements ConfigServer {
    private final static Logger logger = LogManager.getLogger(ConfigHolder.class);

    private final SystemCtx sysCtx;

    public ConfigHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }
}
