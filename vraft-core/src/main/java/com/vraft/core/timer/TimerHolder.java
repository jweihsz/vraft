package com.vraft.core.timer;

import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 02:28
 **/
public class TimerHolder implements TimerService {
    private final static Logger logger = LogManager.getLogger(TimerHolder.class);

    private final SystemCtx sysCtx;
    private final TimerWheel commonTw;

    public TimerHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.commonTw = new TimerWheel(100, 512, 200_0000);
    }

    @Override
    public void shutdown() {
        commonTw.shutdown();
    }

    @Override
    public void startup() throws Exception {
        commonTw.startup();
    }

    @Override
    public boolean removeTimeout(Object task) {
        if (!(task instanceof TimerTask)) {return false;}
        TimerTask tt = (TimerTask)task;
        return tt.cancel();
    }
    
    @Override
    public Object addTimeout(Consumer<Object> apply, Object param, long delay) {
        TimerTask task = ObjectsPool.TIMER_TASK_RECYCLER.get();
        task.apply = apply;
        task.taskParam = param;
        if (commonTw.addTimeout(task, delay)) {
            return task;
        } else {
            task.recycle();
            return null;
        }
    }

}
