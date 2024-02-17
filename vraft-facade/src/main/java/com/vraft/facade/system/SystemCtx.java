package com.vraft.facade.system;

import com.vraft.facade.rpc.RpcService;
import com.vraft.facade.uid.UidService;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/11 11:35
 **/
@Data
public class SystemCtx {
    private UidService uidService;
    private RpcService raftRpcService;
}
