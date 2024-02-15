package com.vraft.facade.rpc;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/15 15:33
 **/
@Data
public class RpcCommand {
    private Exception ex;
    private byte[] body;
    private byte[] header;
    private String clientId;
    private RpcCallback callback;

    public void rest() {
        this.ex = null;
        this.body = null;
        this.header = null;
        this.clientId = null;
        this.callback = null;
    }
}
