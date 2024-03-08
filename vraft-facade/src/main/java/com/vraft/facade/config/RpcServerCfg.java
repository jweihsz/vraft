package com.vraft.facade.config;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/22 18:08
 **/
@Data
public class RpcServerCfg {
    private int rpcSrvPort;
    private String rpcSrvHost;
    private int rpcSrvRcvBufSize;
    private int rpcSrvSndBufSize;
}
