package com.vraft.facade.config;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/8 18:01
 **/
@Data
public class RpcClientCfg {
    private int rpcClientRcvBufSize;
    private int rpcClientSndBufSize;
}
