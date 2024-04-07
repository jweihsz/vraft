package com.vraft.facade.raft.logs;

/**
 * @author jweihsz
 * @version 2024/4/7 15:43
 **/
public enum RaftLogType {
    ENTRY_TYPE_UNKNOWN(0),

    ENTRY_TYPE_NO_OP(1),

    ENTRY_TYPE_DATA(2),

    ENTRY_TYPE_CONFIGURATION(3),
    ;

    private final int type;

    RaftLogType(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static boolean valid(int type) {
        return type > 0 && type < 4;
    }
}
