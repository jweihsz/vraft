package com.vraft.core.timer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * @author jweihsz
 * @version 2024/2/7 20:55
 **/
public class TimerTask {

    public static final int ST_INIT = 0;
    public static final int ST_EXPIRED = 2;
    public static final int ST_CANCELLED = 1;
    private volatile int state = ST_INIT;
    public long deadline;
    public long remainingRounds;
    public TimerTask next, prev;
    public TimerWheel wheel;
    public TimerBucket bucket;
    private Object param;
    private Consumer<TimerTask> apply;
    private static final AtomicIntegerFieldUpdater<TimerTask> STATE_UPDATER;

    public TimerTask() {}

    public TimerTask(TimerWheel wheel) {
        this.wheel = wheel;
    }

    static {
        STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(TimerTask.class, "state");
    }

    public void setBucket(TimerBucket bucket) {
        this.bucket = bucket;
    }

    public boolean cancel() {
        if (compareAndSetState(ST_INIT, ST_CANCELLED)) {
            wheel.getCancels().add(this);
            return true;
        } else {
            return false;
        }
    }

    void remove() {
        TimerBucket bucket = this.bucket;
        if (bucket != null) {
            bucket.remove(this);
        } else {
            wheel.decPending();
        }
    }

    public boolean compareAndSetState(int expected, int state) {
        return STATE_UPDATER.compareAndSet(this, expected, state);
    }

    public int state() {
        return state;
    }

    public boolean isCancelled() {
        return state() == ST_CANCELLED;
    }

    public boolean isExpired() {
        return state() == ST_EXPIRED;
    }

    public void expire() {
        if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
            return;
        }
        try {
            if (apply != null) {
                apply.accept(this);
            }
        } catch (Throwable t) {

        }
    }

}
