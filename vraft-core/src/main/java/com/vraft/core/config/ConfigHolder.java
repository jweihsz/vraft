package com.vraft.core.config;

import java.util.Properties;

import com.vraft.core.utils.OtherUtil;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.config.RaftNodeCfg;
import com.vraft.facade.config.RpcClientCfg;
import com.vraft.facade.config.RpcServerCfg;
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
    private RaftNodeCfg raftNodeCfg;
    private RpcServerCfg rpcServerCfg;
    private RpcClientCfg rpcClientCfg;

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
    public RaftNodeCfg getRaftNodeCfg() {
        return raftNodeCfg;
    }

    @Override
    public RpcServerCfg getRpcServerCfg() {
        return rpcServerCfg;
    }

    @Override
    public RpcClientCfg getRpcClientCfg() {
        return rpcClientCfg;
    }

    @Override
    public String getCfgFile() {
        final String flag = "vraft.env";
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
        rpcServerCfg = new RpcServerCfg();
        OtherUtil.props2Obj(props, rpcServerCfg);

        rpcClientCfg = new RpcClientCfg();
        OtherUtil.props2Obj(props, rpcClientCfg);

        raftNodeCfg = new RaftNodeCfg();
        OtherUtil.props2Obj(props, raftNodeCfg);
    }

    private Properties parseCfg(String path) throws Exception {
        logger.info("config file path:{}", path);
        if (path == null || path.isEmpty()) {return null;}
        final Properties props = new Properties();
        ClassLoader cl = this.getClass().getClassLoader();
        props.load(cl.getResourceAsStream(path));
        return props;
    }
}
