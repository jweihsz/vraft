package com.vraft.core.raft.elect;

import java.util.ArrayList;
import java.util.List;

import com.vraft.facade.common.Pair;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/3/18 17:57
 **/
public class RaftElectBallot {

    private boolean needRest;
    private final SystemCtx sysCtx;
    private int quorum, oldQuorum;
    private final List<Pair<Long, Boolean>> curPeers;
    private final List<Pair<Long, Boolean>> oldPeers;

    public RaftElectBallot(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.needRest = true;
        this.curPeers = new ArrayList<>();
        this.oldPeers = new ArrayList<>();
    }

    public void init(List<Long> cur, List<Long> old) {
        checkReset(cur, old);
        curPeers.forEach(c -> c.setRight(false));
        oldPeers.forEach(c -> c.setRight(false));
    }

    private void checkReset(List<Long> cur, List<Long> old) {
        if (!needRest) {return;}
        curPeers.clear();
        oldPeers.clear();
        cur.forEach(c -> curPeers.add(new Pair<>(c, false)));
        old.forEach(c -> oldPeers.add(new Pair<>(c, false)));
        quorum = cur.size() / 2 + 1;
        oldQuorum = old.size() / 2 + 1;
        needRest = false;
    }

}
