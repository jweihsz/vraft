package com.vraft.facade.raft.logs;

/**
 * @author jweihsz
 * @version 2024/3/30 00:05
 **/
public enum RaftReplicatorType {
    Follower,
    Learner;

    public final boolean isFollower() {
        return this == Follower;
    }

    public final boolean isLearner() {
        return this == Learner;
    }
}
