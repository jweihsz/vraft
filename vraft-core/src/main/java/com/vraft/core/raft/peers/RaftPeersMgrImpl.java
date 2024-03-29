package com.vraft.core.raft.peers;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.config.RaftNodeCfg;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.peers.PeersCfg;
import com.vraft.facade.raft.peers.PeersEntry;
import com.vraft.facade.raft.peers.RaftPeersMgr;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/20 20:41
 **/
public class RaftPeersMgrImpl implements RaftPeersMgr {
    private final static Logger logger = LogManager.getLogger(RaftPeersMgr.class);

    private final SystemCtx sysCtx;
    private final Set<Long> nodeIds;
    private final PeersEntry curEntry;
    private final PeersEntry snapshotEntry;
    private final LinkedList<PeersEntry> hisEntry;

    public RaftPeersMgrImpl(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.curEntry = PeersEntry.build();
        this.hisEntry = new LinkedList<>();
        this.snapshotEntry = PeersEntry.build();
        this.nodeIds = new CopyOnWriteArraySet<>();
    }

    @Override
    public void init() throws Exception {
        ConfigServer cfgSrv = sysCtx.getCfgSvs();
        final RaftNodeCfg cfg = cfgSrv.getRaftNodeCfg();
        parseCurCfg(curEntry.getCurConf(), cfg);
    }

    @Override
    public Set<Long> getAllNodeIds() {return nodeIds;}

    @Override
    public PeersEntry getCurEntry() {return curEntry;}

    @Override
    public PeersEntry getSnapshotEntry() {return snapshotEntry;}

    @Override
    public LinkedList<PeersEntry> getHisEntry() {return hisEntry;}

    @Override
    public boolean isLearner(long nodeId) {
        final PeersCfg curConf = curEntry.getCurConf();
        return curConf.getLearners().containsKey(nodeId);
    }

    private void parseCurCfg(PeersCfg cur, RaftNodeCfg cfg) {
        RequireUtil.nonNull(cfg.getRaftSelf());
        RequireUtil.nonNull(cfg.getRaftPeers());
        Map<Long, RaftNodeMate> peers = cur.getPeers();
        RaftNodeMate mate = buildMate(cfg.getRaftSelf());
        peers.put(mate.getNodeId(), mate);
        nodeIds.add(mate.getNodeId());
        String[] ss = cfg.getRaftPeers().split(",");
        for (String s : ss) {
            mate = buildMate(s);
            peers.put(mate.getNodeId(), mate);
            nodeIds.add(mate.getNodeId());
        }
    }

    private RaftNodeMate buildMate(String address) {
        String[] ss = address.split(":");
        RequireUtil.isTrue(ss.length == 2);
        int port = Integer.parseInt(ss[1]);
        long nodeId = MathUtil.address2long(ss[0], port);
        return new RaftNodeMate(nodeId, address);
    }

}
