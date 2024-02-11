package com.vraft.facade.system;

import com.vraft.facade.rpc.RpcService;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/11 11:35
 **/
@Data
public class SystemCtx {

    private RpcService raftRpcService;

}
