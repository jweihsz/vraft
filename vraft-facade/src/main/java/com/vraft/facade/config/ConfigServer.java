package com.vraft.facade.config;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/21 17:43
 **/
public interface ConfigServer extends LifeCycle {

    default String getCfgFile() {return null;}

    default RaftNodeCfg getRaftNodeCfg() {return null;}

    default RpcServerCfg getRpcServerCfg() {return null;}

    default RpcClientCfg getRpcClientCfg() {return null;}

}
