package com.vraft.facade.raft.node;

/**
 * @author jweihsz
 * @version 2024/2/26 15:48
 **/
public enum RaftNodeRole {
    LEADER,
    CANDIDATE,
    FOLLOWER,
    LEARNER,
    QUITTER,
    UNKNOW,
}
