package com.vraft.core.pool;

import com.vraft.core.rpc.RpcCmdExt;
import com.vraft.core.timer.TimerTask;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import io.netty.util.Recycler;

/**
 * @author jweihsz
 * @version 2024/2/8 02:13
 **/
public class ObjectsPool {
    private ObjectsPool() {}

    private static final ThreadLocal<RaftVoteReq> voteReq;
    private static final ThreadLocal<RaftVoteResp> voteResp;
    private static final ThreadLocal<RaftInnerCmd> innerCmd;

    static {
        voteReq = new ThreadLocal<>();
        voteResp = new ThreadLocal<>();
        innerCmd = new ThreadLocal<>();
    }

    public static final Recycler<TimerTask> TIMER_TASK_RECYCLER = new Recycler<TimerTask>() {
        @Override
        protected TimerTask newObject(Handle<TimerTask> handle) {
            return new TimerTask(handle);
        }
    };

    public static final Recycler<RpcCmdExt> RPC_CMD_RECYCLER = new Recycler<RpcCmdExt>() {
        @Override
        protected RpcCmdExt newObject(Handle<RpcCmdExt> handle) {
            return new RpcCmdExt(handle);
        }
    };

    public static RaftVoteReq getVoteReqObj() {
        RaftVoteReq req = voteReq.get();
        if (req != null) {return req;}
        req = new RaftVoteReq();
        voteReq.set(req);
        return req;
    }

    public static RaftVoteResp getVoteRespObj() {
        RaftVoteResp resp = voteResp.get();
        if (resp != null) {return resp;}
        resp = new RaftVoteResp();
        voteResp.set(resp);
        return resp;
    }

    public static RaftInnerCmd getInnerCmdObj() {
        RaftInnerCmd cmd = innerCmd.get();
        if (cmd != null) {return cmd;}
        cmd = new RaftInnerCmd();
        innerCmd.set(cmd);
        return cmd;
    }

}
