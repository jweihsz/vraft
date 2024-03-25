package com.vraft.facade.common;

/**
 * @author jweihsz
 * @version 2024/3/25 15:43
 **/
public class Code {
    private Code() {}

    public static int SUCCESS = 200;

    public static int RAFT_ERROR_NODE = 1001;
    public static int RAFT_NOT_ACTIVE = 1002;
    public static int RAFT_NOT_MEMBER = 1003;
    public static int RAFT_VALID_LEADER = 1004;
    public static int RAFT_TERM_SMALLER = 1005;

}
