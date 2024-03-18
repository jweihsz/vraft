package com.vraft.facade.raft.fsm;

/**
 * @author jweihsz
 * @version 2024/3/18 20:21
 **/
public interface FsmCallback {

    default boolean onStopFollowing(long leaderId, long curTerm) {return false;}
    
    default boolean onStartFollowing(long leaderId, long curTerm) {return false;}

}
