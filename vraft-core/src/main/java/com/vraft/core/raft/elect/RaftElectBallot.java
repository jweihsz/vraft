package com.vraft.core.raft.elect;

import java.util.ArrayList;
import java.util.List;

import com.vraft.facade.common.Pair;
import com.vraft.facade.raft.peers.PeersCfg;
import com.vraft.facade.raft.peers.PeersEntry;
import com.vraft.facade.system.SystemCtx;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/18 17:57
 **/
@Data
public class RaftElectBallot {
    private final static Logger logger = LogManager.getLogger(RaftElectBallot.class);

    private final SystemCtx sysCtx;
    private int quorum, oldQuorum;
    private final List<Pair<Long, Boolean>> curPeers;
    private final List<Pair<Long, Boolean>> oldPeers;

    public RaftElectBallot(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.curPeers = new ArrayList<>();
        this.oldPeers = new ArrayList<>();
    }

    public void init(PeersEntry entry, boolean init) {
        if (init) {reInit(entry.getCurConf(), entry.getOldConf());}
        curPeers.forEach(c -> c.setRight(false));
        oldPeers.forEach(c -> c.setRight(false));
        quorum = curPeers.isEmpty() ? 0 : curPeers.size() / 2 + 1;
        oldQuorum = oldPeers.isEmpty() ? 0 : oldPeers.size() / 2 + 1;
    }

    public void doGrant(long nodeId) {
        for (Pair<Long, Boolean> p : curPeers) {
            if (p.getLeft() != nodeId) {continue;}
            if (!p.getRight()) {
                p.setRight(true);
                quorum -= 1;
            }
            break;
        }
        for (Pair<Long, Boolean> p : oldPeers) {
            if (p.getLeft() != nodeId) {continue;}
            if (!p.getRight()) {
                p.setRight(true);
                oldQuorum -= 1;
            }
            break;
        }
    }

    public boolean isGranted() {
        logger.info("quorum={},oldQuorum={}"
            , quorum, oldQuorum);
        return this.quorum <= 0
            && this.oldQuorum <= 0;
    }

    private void reInit(PeersCfg cur, PeersCfg old) {
        curPeers.clear();
        oldPeers.clear();
        cur.getPeers().keySet().forEach(
            c -> curPeers.add(new Pair<>(c, false))
        );
        old.getPeers().keySet().forEach(
            c -> oldPeers.add(new Pair<>(c, false))
        );
    }

}
