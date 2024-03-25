package com.vraft.core.raft.handler;

import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/14 20:43
 **/
public class RaftVoteRespHandler implements RpcProcessor {

    private final static Logger logger = LogManager.getLogger(RaftVoteRespHandler.class);

    private final SystemCtx sysCtx;

    public RaftVoteRespHandler(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void handle(long connectId, long groupId, long nodeId, long msgId,
        byte[] header, byte[] body, boolean hasNext) throws Exception {
        logger.info("RaftVoteRespHandler");
    }

    @Override
    public String uid() {
        return RaftVoteResp.class.getName();
    }
}
