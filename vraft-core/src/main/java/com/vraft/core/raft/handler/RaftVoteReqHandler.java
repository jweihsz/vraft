package com.vraft.core.raft.handler;

import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/14 20:41
 **/
public class RaftVoteReqHandler implements RpcProcessor {

    private final static Logger logger = LogManager.getLogger(RaftVoteReqHandler.class);

    private final SystemCtx sysCtx;

    public RaftVoteReqHandler(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void handle(long connectId, long msgId,
        byte[] header, byte[] body, boolean hasNext) throws Exception {
        Serializer sz = sysCtx.getSerializerMgr().get(SerializerEnum.KRYO_ID);
        RaftVoteReq req = sz.deserialize(body, RaftVoteReq.class);
        logger.info("RaftVoteReq :{},hasNext:{}", req, hasNext);
    }

    @Override
    public String uid() {
        return RaftVoteReq.class.getName();
    }

}
