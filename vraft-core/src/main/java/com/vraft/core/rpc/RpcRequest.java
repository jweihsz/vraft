package com.vraft.core.rpc;

import com.vraft.facade.rpc.RpcCommand;
import io.netty.util.Recycler.Handle;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jweihsz
 * @version 2024/2/15 15:57
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class RpcRequest extends RpcCommand {
    private long seqId;
    public transient Handle<RpcRequest> handle;

    public RpcRequest() {
        super();
    }

    public RpcRequest(Handle<RpcRequest> handle) {
        super();
        this.handle = handle;
    }

    private void reset() {
        super.rest();
        this.seqId = 0L;
    }

    public void recycle() {
        super.rest();
        this.reset();
        this.handle.recycle(this);
    }

}
