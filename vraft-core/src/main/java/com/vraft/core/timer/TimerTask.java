package com.vraft.core.timer;

import static com.vraft.core.timer.TimerConsts.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.netty.util.Recycler.Handle;

/**
 * @author jweihsz
 * @version 2024/2/7 20:55
 **/
public class TimerTask {
    public Object taskCtx;
    public TimerWheel wheel;
    public TimerBucket bucket;
    public TimerTask next, prev;
    public Consumer<TimerTask> apply;
    public long deadline, remaining;
    private final AtomicInteger state;
    public transient Handle<TimerTask> handle;

    public TimerTask() {
        this.state = new AtomicInteger(ST_INIT);
    }

    public TimerTask(Handle<TimerTask> handle) {
        this.handle = handle;
        this.state = new AtomicInteger(ST_INIT);
    }

    public boolean cancel() {
        if (set(ST_INIT, ST_CANCELLED)) {
            wheel.addCancel(this);
            return true;
        } else {
            return false;
        }
    }

    public void remove() {
        try {
            bucket.remove(this);
            wheel.decPending();
            recycle();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int state() {
        return state.get();
    }

    public boolean isCancelled() {
        return state() == ST_CANCELLED;
    }

    public boolean isExpired() {
        return state() == ST_EXPIRED;
    }

    public void expire() {
        if (!set(ST_INIT, ST_EXPIRED)) {
            return;
        }
        if (apply != null) {
            apply.accept(this);
        }
    }

    private boolean set(int o, int n) {
        return state.compareAndSet(o, n);
    }

    public void recycle() {
        this.next = null;
        this.prev = null;
        this.apply = null;
        this.deadline = 0L;
        this.remaining = 0L;
        this.taskCtx = null;
        this.bucket = null;
        this.state.set(ST_INIT);
        this.handle.recycle(this);
    }

}
