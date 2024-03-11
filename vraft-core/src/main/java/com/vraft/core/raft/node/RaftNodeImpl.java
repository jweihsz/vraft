package com.vraft.core.raft.node;

import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.node.RaftNode;
import com.vraft.facade.raft.node.RaftNodeMate;
import com.vraft.facade.system.SystemCtx;

/**
 * @author jweihsz
 * @version 2024/3/11 11:33
 **/
public class RaftNodeImpl implements RaftNode {

    private final SystemCtx sysCtx;
    private final RaftNodeMate mate;

    public RaftNodeImpl(SystemCtx sysCtx, RaftNodeMate mate) {
        this.mate = mate;
        this.sysCtx = sysCtx;
    }

    @Override
    public void shutdown() {}

    @Override
    public void init() throws Exception {}

    @Override
    public void startup() throws Exception {
        valid(sysCtx);
    }

    private void valid(SystemCtx sysCtx) {
        RequireUtil.nonNull(sysCtx);
        RequireUtil.nonNull(sysCtx.getRpcSrv());
        RequireUtil.nonNull(sysCtx.getRpcClient());
        RequireUtil.nonNull(sysCtx.getTimerSvs());
    }

}
