package com.vraft.core.raft.peers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.config.ConfigServer;
import com.vraft.facade.config.RaftNodeCfg;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.raft.peers.PeersCfg;
import com.vraft.facade.raft.peers.PeersEntry;
import com.vraft.facade.raft.peers.PeersService;
import com.vraft.facade.system.SystemCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/20 20:41
 **/
public class PeersManager implements PeersService {
    private final static Logger logger = LogManager.getLogger(PeersService.class);

    private final SystemCtx sysCtx;
    private final PeersEntry curCfg;
    private final PeersEntry snapshot;
    private final LinkedList<PeersEntry> dataList;

    public PeersManager(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.curCfg = PeersEntry.build();
        this.snapshot = PeersEntry.build();
        this.dataList = new LinkedList<>();
    }

    @Override
    public void init() throws Exception {
        ConfigServer cfgSrv = sysCtx.getCfgSvs();
        final RaftNodeCfg cfg = cfgSrv.getRaftNodeCfg();
        parseCurCfg(curCfg.getCurConf(), cfg);
    }

    @Override
    public PeersEntry getCurCfg() {return curCfg;}

    @Override
    public PeersEntry getSnapshot() {return snapshot;}

    @Override
    public LinkedList<PeersEntry> getLists() {return dataList;}

    private void parseCurCfg(PeersCfg cur, RaftNodeCfg cfg) {
        RequireUtil.nonNull(cfg.getRaftSelf());
        RequireUtil.nonNull(cfg.getRaftPeers());
        List<RaftNodeMate> peers = cur.getPeers();
        addPeer(peers, buildMate(cfg.getRaftSelf()));
        String[] ss = cfg.getRaftPeers().split(",");
        for (String s : ss) {addPeer(peers, buildMate(s));}
    }

    private RaftNodeMate buildMate(String address) {
        String[] ss = address.split(":");
        RequireUtil.isTrue(ss.length == 2);
        int port = Integer.parseInt(ss[1]);
        long nodeId = MathUtil.address2long(ss[0], port);
        return new RaftNodeMate(nodeId, address);
    }

    private RaftNodeMate getPeer(List<RaftNodeMate> lists, long nodeId) {
        if (lists == null || lists.isEmpty()) {return null;}
        for (RaftNodeMate mate : lists) {
            if (mate.getNodeId() == nodeId) {return mate;}
        }
        return null;
    }

    private void removePeer(List<RaftNodeMate> lists, long nodeId) {
        if (lists == null || lists.isEmpty()) {return;}
        final Iterator<RaftNodeMate> it = lists.iterator();
        while (it.hasNext()) {
            RaftNodeMate mate = it.next();
            if (mate.getNodeId() != nodeId) {continue;}
            it.remove();
            break;
        }
    }

    private void addPeer(List<RaftNodeMate> lists, RaftNodeMate mate) {
        if (mate == null) {return;}
        if (lists == null || lists.isEmpty()) {return;}
        removePeer(lists, mate.getNodeId());
        lists.add(mate);
    }

}
