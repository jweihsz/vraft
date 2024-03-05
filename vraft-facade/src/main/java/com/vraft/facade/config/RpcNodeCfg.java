package com.vraft.facade.config;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/22 18:08
 **/
@Data
public class RpcNodeCfg {
    private int rpcPort;
    private String rpcHost;
    private int rpcRcvBufSize;
    private int rpcSndBufSize;
}
