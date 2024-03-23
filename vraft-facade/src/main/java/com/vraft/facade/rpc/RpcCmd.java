package com.vraft.facade.rpc;

import com.vraft.facade.common.CallBack;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/21 15:48
 **/
@Data
public class RpcCmd {
    private byte biz;
    private byte type;
    private Object ext;
    private String uid;
    private long msgId;
    private long userId;
    private long groupId;
    private byte[] body;
    private byte[] header;
    private long timeout;
    private Throwable ex;
    private CallBack callBack;

    public void rest() {
        this.type = -1;
        this.biz = -1;
        this.msgId = -1;
        this.userId = -1;
        this.uid = null;
        this.ext = null;
        this.ex = null;
        this.body = null;
        this.header = null;
        this.callBack = null;
        this.timeout = -1L;
    }
}
