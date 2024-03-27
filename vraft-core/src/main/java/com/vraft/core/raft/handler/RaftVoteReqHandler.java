package com.vraft.core.raft.handler;

import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
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
    public void handle(long connectId, long groupId, long nodeId, long msgId,
        byte[] header, byte[] body, boolean hasNext) throws Exception {
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftVoteReq req = sz.deserialize(body, RaftVoteReq.class);
        logger.info("RaftVoteReq :{},hasNext:{}", req, hasNext);
        processPreVoteReq(req, groupId, nodeId, msgId);
    }

    @Override
    public String uid() {return RaftVoteReq.class.getName();}

    private void processPreVoteReq(RaftVoteReq req,
        long groupId, long nodeId, long msgId) throws Exception {
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        RaftNode node = mgr.getNodeMate(groupId, nodeId);
        if (node == null) {return;}
        RaftElectService raftElect = null;
        raftElect = node.getOpts().getRaftElect();
        RpcClient client = sysCtx.getRpcClient();
        byte[] body = raftElect.processPreVoteReq(req);
        long userId = client.doConnect(req.getSrcIp());
        if (userId < 0) {return;}
        String uid = RaftVoteResp.class.getName();
        client.oneWay(userId, msgId, (byte)0, req.getGroupId(),
            req.getNodeId(), uid, null, body);
    }

}
