package com.vraft.core.raft.logs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.utils.OtherUtil;
import com.vraft.facade.raft.logs.RaftReplicator;
import com.vraft.facade.raft.logs.RaftReplicatorOpts;
import com.vraft.facade.raft.logs.RaftReplicatorType;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeCtx;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.peers.RaftPeersMgr;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/30 00:07
 **/
public class RaftReplicatorImpl implements RaftReplicator {
    private final static Logger logger = LogManager.getLogger(RaftReplicatorImpl.class);

    private final SystemCtx sysCtx;
    private final RaftReplicatorOpts opts;
    private final Map<Long, RaftReplicatorType> fails;
    private final Map<Long, RaftReplicatorType> nodes;

    public RaftReplicatorImpl(SystemCtx sysCtx, RaftReplicatorOpts opts) {
        this.opts = opts;
        this.sysCtx = sysCtx;
        this.fails = new ConcurrentHashMap<>();
        this.nodes = new ConcurrentHashMap<>();
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {}

    @Override
    public void shutdown() {}

    private RaftReplicatorOpts newOpts(RaftNode node) {
        return new RaftReplicatorOpts();
    }

    @Override
    public boolean resetTerm(final long newTerm) {
        if (newTerm <= this.opts.getTerm()) {return false;}
        this.opts.setTerm(newTerm);
        return true;
    }

    @Override
    public boolean addReplicator(long nodeId,
        RaftReplicatorType type, boolean sync) {
        fails.remove(nodeId);
        if (nodes.containsKey(nodeId)) {return true;}
        RaftNodeCtx nodeCtx = opts.getNode().getNodeCtx();
        final RaftPeersMgr peersMgr = nodeCtx.getPeersMgr();
        final RpcClient rpcClient = sysCtx.getRpcClient();
        RaftNodeMate mate = peersMgr.getCurEntry().getNode(nodeId);
        if (rpcClient.doConnect(mate.getSrcIp()) < 0) {
            fails.put(nodeId, type);
            return false;
        }
        long gmt = OtherUtil.getSysMs();
        opts.setLastRpcSendTimestamp(gmt);
        nodes.put(nodeId, type);
        return true;
    }
    
}
