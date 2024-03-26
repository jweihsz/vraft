package com.vraft.core.raft.elect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vraft.facade.common.Pair;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.system.SystemCtx;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/18 17:57
 **/
@Data
public class RaftElectBallot {

    private final SystemCtx sysCtx;
    private int quorum, oldQuorum;
    private final List<Pair<Long, Boolean>> curPeers;
    private final List<Pair<Long, Boolean>> oldPeers;

    public RaftElectBallot(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.curPeers = new ArrayList<>();
        this.oldPeers = new ArrayList<>();
    }

    public void init(Map<Long, RaftNodeMate> cur,
        Map<Long, RaftNodeMate> old, boolean needRest) {
        checkReset(cur, old, needRest);
        curPeers.forEach(c -> c.setRight(false));
        oldPeers.forEach(c -> c.setRight(false));
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
        return this.quorum <= 0
            && this.oldQuorum <= 0;
    }

    private void checkReset(Map<Long, RaftNodeMate> cur,
        Map<Long, RaftNodeMate> old, boolean needRest) {
        if (!needRest) {return;}
        curPeers.clear();
        oldPeers.clear();
        cur.keySet().forEach(c -> curPeers.add(new Pair<>(c, false)));
        old.keySet().forEach(c -> oldPeers.add(new Pair<>(c, false)));
        quorum = cur.size() / 2 + 1;
        oldQuorum = old.size() / 2 + 1;
    }

}
