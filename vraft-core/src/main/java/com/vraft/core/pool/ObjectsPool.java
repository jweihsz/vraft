package com.vraft.core.pool;

import com.vraft.core.rpc.RpcCmdExt;
import com.vraft.core.timer.TimerTask;
import com.vraft.facade.raft.elect.RaftInnerCmd;
import com.vraft.facade.raft.elect.RaftVoteReq;
import com.vraft.facade.raft.elect.RaftVoteResp;
import io.netty.util.Recycler;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * @author jweihsz
 * @version 2024/2/8 02:13
 **/
public class ObjectsPool {
    private ObjectsPool() {}

    private static final FastThreadLocal<byte[]> bytes16;
    private static final FastThreadLocal<byte[]> bytes25;
    private static final FastThreadLocal<RaftVoteReq> voteReq;
    private static final FastThreadLocal<RaftVoteResp> voteResp;
    private static final FastThreadLocal<RaftInnerCmd> innerCmd;

    static {
        bytes16 = new FastThreadLocal<>();
        bytes25 = new FastThreadLocal<>();
        voteReq = new FastThreadLocal<>();
        voteResp = new FastThreadLocal<>();
        innerCmd = new FastThreadLocal<>();
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

    public static byte[] getBytes16Obj() {
        byte[] bs = bytes16.get();
        if (bs != null) {return bs;}
        bs = new byte[16];
        bytes16.set(bs);
        return bs;
    }

    public static byte[] getBytes25Obj() {
        byte[] bs = bytes25.get();
        if (bs != null) {return bs;}
        bs = new byte[25];
        bytes16.set(bs);
        return bs;
    }

}
