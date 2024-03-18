package com.vraft.core.raft.proc;

import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/14 20:41
 **/
public class RaftVoteReqProc implements RpcProcessor {

    private final static Logger logger = LogManager.getLogger(RaftVoteReqProc.class);

    private final SystemCtx sysCtx;

    public RaftVoteReqProc(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void handle(long connectId, long msgId,
        byte[] header, byte[] body) throws Exception {

    }

    @Override
    public String uid() {
        return RaftVoteReqProc.class.getName();
    }

}
