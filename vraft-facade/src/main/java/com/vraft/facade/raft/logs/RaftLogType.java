package com.vraft.facade.raft.logs;

/**
 * @author jweihsz
 * @version 2024/4/7 15:43
 **/
public enum RaftLogType {
    UNKNOWN((byte)0),

    NO_OP((byte)1),

    ENTRY_DATA((byte)2),

    ENTRY_CONF((byte)3),
    ;

    private final byte type;

    RaftLogType(final byte type) {this.type = type;}

    public byte getType() {return type;}

    public static boolean valid(byte type) {
        return type > 0 && type < 4;
    }
}
