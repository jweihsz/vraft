package com.vraft.facade.raft.node;

import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;

/**
 * @author jweihsz
 * @version 2024/3/11 11:31
 **/
public interface RaftNode extends LifeCycle {

    RaftNodeOpts getOpts();

    byte[] processPreVoteReq(RaftVoteReq req) throws Exception;

    void processPreVoteResp(RaftVoteResp resp) throws Exception;
}
