package com.vraft.facade.rpc;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/10 11:35
 **/
@Data
public class RpcMsgBase {
    private short magic;
    private int totalLen;

    private byte version;
    private byte msgType;
    private long msgId;

    private int uidLen;
    private int uidIndex;

    private int hLen;
    private int hIndex;

    private int bLen;
    private int bIndex;
}
