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
    private final TimerWheel tw;

    public TimerHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.tw = new TimerWheel(200_0000);
    }

    @Override
    public void shutdown() {
        tw.shutdown();
    }

    @Override
    public void startup() throws Exception {
        tw.startup();
    }

    @Override
    public boolean removeTimeout(Object task) {
        if (task instanceof TimerTask) {
            TimerTask tt = (TimerTask)task;
            return tt.cancel();
        } else {
            return false;
        }
    }

    @Override
    public Object addTimeout(Consumer<Object> apply, Object param, long delay) {
        TimerTask task = ObjectsPool.TIMER_TASK_RECYCLER.get();
        task.setApply(apply);
        task.setParams(param);
        if (tw.addTimeout(task, delay)) {return task;}
        task.recycle();
        return null;
    }
}
