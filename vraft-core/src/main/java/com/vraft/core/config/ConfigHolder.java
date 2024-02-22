package com.vraft.core.config;

import java.util.Properties;

import com.vraft.core.utils.OtherUtil;
import com.vraft.facade.config.CfgRpcNode;
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

    private CfgRpcNode cfgRpcNode;
    private final SystemCtx sysCtx;

    public ConfigHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void shutdown() {}

    @Override
    public void startup() throws Exception {
        loadSysCfg();
    }

    @Override
    public CfgRpcNode getCfgRpcNode() {
        return cfgRpcNode;
    }

    @Override
    public String getCfgFile() {
        final String flag = "raft.env";
        String env = System.getProperty(flag);
        if (env == null || env.isEmpty()) {
            env = System.getenv(flag);
        }
        if (env == null || env.isEmpty()) {
            env = "daily";
        }
        return env + ".properties";
    }

    private void loadSysCfg() throws Exception {
        Properties props = parseCfg(getCfgFile());
        cfgRpcNode = new CfgRpcNode();
        OtherUtil.props2Obj(props, cfgRpcNode);
    }

    private Properties parseCfg(String path) throws Exception {
        if (path == null || path.isEmpty()) {return null;}
        final Properties props = new Properties();
        ClassLoader cl = this.getClass().getClassLoader();
        props.load(cl.getResourceAsStream(path));
        return props;
    }
}
