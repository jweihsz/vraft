package com.vraft.core.raft.elect;

import java.util.Map;

import com.vraft.facade.raft.elect.RaftElectService;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import com.vraft.facade.raft.node.RaftNodeGroup;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.serializer.Serializer;
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

    public void doVoteReq() {
        RaftNodeGroup nodeGroup = sysCtx.getNodeGroup();
        Map<Long, Map<Long, RaftNodeMate>> map = nodeGroup.getAll();
        if (map == null || map.isEmpty()) {return;}
        map.values().forEach(this::doVoteReqGroup);
    }

    private void doVoteReqGroup(Map<Long, RaftNodeMate> group) {
        if (group == null || group.isEmpty()) {return;}
        group.values().forEach(this::doVoteReqNode);
    }

    private void doVoteReqNode(RaftNodeMate node) {
        try {
            final Serializer sz = sysCtx.getSerializer();
            final RpcClient client = sysCtx.getRpcClient();
            long userId = client.doConnect(node.getSrcIp());
            if (userId < 0) {return;}
            RaftVoteReq req = getVoteReqObj();
            req.setCurTerm(0L);
            req.setLastLogId(0L);
            req.setLastTerm(0L);
            req.setNodeId(node.getNodeId());
            req.setGroupId(node.getGroupId());
            req.setSrcIp(node.getSrcIp());
            byte[] body = sz.serialize(req);
            final String uid = RaftVoteReq.class.getName();
            client.oneWay(userId, (byte)0, uid, null, body);
        } catch (Exception ex) {ex.printStackTrace();}
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
