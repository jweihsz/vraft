package com.vraft.core.raft.handler;

import com.vraft.facade.raft.elect.RaftElectMgr;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeCmd;
import com.vraft.facade.raft.node.RaftNodeMgr;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/26 21:10
 **/
public class RaftInnerCmdHandler implements RpcProcessor {

    private final static Logger logger = LogManager.getLogger(RaftInnerCmdHandler.class);

    private final SystemCtx sysCtx;

    public RaftInnerCmdHandler(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public String uid() {return RaftInnerCmd.class.getName();}

    @Override
    public void handle(long connectId, long groupId, long nodeId,
        long msgId, byte[] header, byte[] body, boolean hasNext)
        throws Exception {
        SerializerMgr szMgr = sysCtx.getSerializerMgr();
        Serializer sz = szMgr.get(SerializerEnum.KRYO_ID);
        RaftInnerCmd pkg = sz.deserialize(body, RaftInnerCmd.class);
        final int cmd = pkg.getCmd();
        if (RaftNodeCmd.CMD_DO_PRE_VOTE == cmd) {
            processPreVoteCmd(groupId, nodeId);
        } else if (RaftNodeCmd.CMD_DO_FOR_VOTE == cmd) {
            processForVoteCmd(groupId, nodeId);
        }
    }

    private void processPreVoteCmd(long groupId,
        long nodeId) throws Exception {
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        RaftNode node = mgr.getNodeMate(groupId, nodeId);
        if (node == null) {return;}
        RaftElectMgr raftElect = null;
        raftElect = node.getOpts().getElectMgr();
        raftElect.doPreVote();
    }

    private void processForVoteCmd(long groupId,
        long nodeId) throws Exception {
        final RaftNodeMgr mgr = sysCtx.getRaftNodeMgr();
        RaftNode node = mgr.getNodeMate(groupId, nodeId);
        if (node == null) {return;}
        RaftElectMgr raftElect = null;
        raftElect = node.getOpts().getElectMgr();
        raftElect.doForVote();
    }

}
