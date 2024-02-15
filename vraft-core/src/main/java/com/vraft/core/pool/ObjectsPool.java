package com.vraft.core.pool;

import com.vraft.core.rpc.RpcRequest;
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

    public static final Recycler<RpcRequest> RPC_REQUEST_RECYCLER = new Recycler<RpcRequest>() {
        @Override
        protected RpcRequest newObject(Handle<RpcRequest> handle) {
            return new RpcRequest(handle);
        }
    };
}
