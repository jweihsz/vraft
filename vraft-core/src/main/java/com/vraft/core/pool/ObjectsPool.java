package com.vraft.core.pool;

import com.vraft.core.rpc.RpcCmd;
import com.vraft.core.timer.TimerTask;
import io.netty.util.Recycler;

/**
 * @author jweihsz
 * @version 2024/2/8 02:13
 **/
public class ObjectsPool {
    private ObjectsPool() {}

    public static final Recycler<TimerTask> TIMER_TASK_RECYCLER = new Recycler<TimerTask>() {
        @Override
        protected TimerTask newObject(Handle<TimerTask> handle) {
            return new TimerTask(handle);
        }
    };

    public static final Recycler<RpcCmd> RPC_CMD_RECYCLER = new Recycler<RpcCmd>() {
        @Override
        protected RpcCmd newObject(Handle<RpcCmd> handle) {
            return new RpcCmd(handle);
        }
    };

}
