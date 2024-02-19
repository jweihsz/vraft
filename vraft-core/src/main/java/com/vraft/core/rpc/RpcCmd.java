package com.vraft.core.rpc;

import com.vraft.facade.common.CallBack;
import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/18 16:09
 **/
@Data
public class RpcCmd {
    private byte req;
    private byte ty;
    private Object ext;
    private String uid;
    private long msgId;
    private long userId;
    private byte[] body;
    private byte[] header;
    private long timeout;
    private Throwable ex;
    private CallBack callBack;
    private transient Handle<RpcCmd> handle;

    public RpcCmd() {}

    public RpcCmd(Handle<RpcCmd> handle) {
        this.handle = handle;
    }

    public void recycle() {
        this.ty = -1;
        this.req = -1;
        this.msgId = -1;
        this.userId = -1;
        this.uid = null;
        this.ext = null;
        this.ex = null;
        this.body = null;
        this.header = null;
        this.callBack = null;
        this.timeout = -1L;
        this.handle.recycle(this);
    }

}
