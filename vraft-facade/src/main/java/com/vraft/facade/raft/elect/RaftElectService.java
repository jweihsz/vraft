package com.vraft.facade.raft.elect;

/**
 * @author jweihsz
 * @version 2024/3/27 09:50
 **/
public interface RaftElectService {

    void startVote(Boolean isPre);

    void doPreVote() throws Exception;

    void doForVote() throws Exception;

    void processPreVoteResp(RaftVoteResp resp) throws Exception;

    void processForVoteResp(RaftVoteResp resp) throws Exception;

    byte[] processForVoteReq(RaftVoteReq req) throws Exception;

    byte[] processPreVoteReq(RaftVoteReq req) throws Exception;
}
