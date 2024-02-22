package com.vraft.facade.config;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/21 17:43
 **/
public interface ConfigServer extends LifeCycle {

    default CfgRpcNode getCfgRpcNode() {return null;}

}
