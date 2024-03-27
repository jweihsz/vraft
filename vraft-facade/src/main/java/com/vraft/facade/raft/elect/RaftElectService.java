package com.vraft.facade.raft.elect;

/**
 * @author jweihsz
 * @version 2024/3/27 09:50
 **/
public interface RaftElectService {

    void startVote(Boolean isPre);

    void doVote(boolean isPre) throws Exception;

    void processPreVoteResp(RaftVoteResp resp) throws Exception;

    byte[] processPreVoteReq(RaftVoteReq req) throws Exception;
}
