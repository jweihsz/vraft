package com.vraft.core.rpc;

import com.vraft.facade.rpc.RpcCmd;
import io.netty.util.Recycler.Handle;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jweihsz
 * @version 2024/2/18 16:09
 **/

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcCmdExt extends RpcCmd {
    private transient Handle<RpcCmdExt> handle;

    public RpcCmdExt() {}

    public RpcCmdExt(Handle<RpcCmdExt> handle) {
        this.handle = handle;
    }

    public void recycle() {
        super.rest();
        this.handle.recycle(this);
    }
}
