package com.vraft.core.raft.elect;

import java.util.Map;

import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/26 15:12
 **/
public class RaftElectHolder implements RaftElectService {
    private final static Logger logger = LogManager.getLogger(RaftElectHolder.class);

    private final SystemCtx sysCtx;
    private static final ThreadLocal<RaftVoteReq> voteReq;
    private static final ThreadLocal<RaftVoteResp> voteResp;

    static {
        voteReq = new ThreadLocal<>();
        voteResp = new ThreadLocal<>();
    }

    public RaftElectHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    private void sendVoteReq(RaftVoteReq req, RaftNodeMate self,
        Map<Long, RaftNodeMate> group) throws Exception {
        final SerializerMgr szMgr = sysCtx.getSerializerMgr();
        final Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        final RpcClient client = sysCtx.getRpcClient();
        final String uid = RaftVoteReq.class.getName();
        final byte[] body = sz.serialize(req);
        for (RaftNodeMate entry : group.values()) {
            if (self.getNodeId() == entry.getNodeId()) {continue;}
            long userId = client.doConnect(entry.getSrcIp());
            if (userId < 0) {return;}
            client.oneWay(userId, (byte)0, uid, null, body);
        }
    }

    private RaftVoteReq buildVoteReq(RaftNodeMate self) {
        RaftVoteReq req = null;
        req = getVoteReqObj();
        req.setCurTerm(0L);
        req.setLastLogId(0L);
        req.setLastTerm(0L);
        req.setNodeId(self.getNodeId());
        req.setGroupId(self.getGroupId());
        req.setSrcIp(self.getSrcIp());
        return req;
    }

    private RaftVoteReq getVoteReqObj() {
        RaftVoteReq req = voteReq.get();
        if (req != null) {return req;}
        req = new RaftVoteReq();
        voteReq.set(req);
        return req;
    }

    private RaftVoteResp getVoteRespObj() {
        RaftVoteResp resp = voteResp.get();
        if (resp != null) {return resp;}
        resp = new RaftVoteResp();
        voteResp.set(resp);
        return resp;
    }

}
