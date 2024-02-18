package com.vraft.core.timer;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.vraft.core.utils.MathUtil;
import com.vraft.core.utils.OtherUtil;
import io.netty.util.internal.PlatformDependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.vraft.core.timer.TimerConsts.ST_CANCELLED;
import static com.vraft.core.timer.TimerConsts.WORKER_INIT;
import static com.vraft.core.timer.TimerConsts.WORKER_SHUTDOWN;
import static com.vraft.core.timer.TimerConsts.WORKER_STARTED;

/**
 * @author jweihsz
 * @version 2024/2/7 20:55
 **/
public class TimerWheel {
    private final static Logger logger = LogManager.getLogger(TimerWheel.class);

    private Thread workerThread;
    private final AtomicLong pendingNum;
    private final TimerBucket[] buckets;
    private final AtomicInteger workerState;
    private long curTick = 0, startTime = 1;
    private final long maxPending, tickDuration;
    private final Queue<TimerTask> cancels, timeouts, unProcess;

    public TimerWheel(long maxPending) {
        this(100, 512, maxPending);
    }

    public TimerWheel(long tickDuration, int ticksPerWheel, long maxPending) {
        this.maxPending = maxPending;
        this.buckets = createBucket(ticksPerWheel);
        this.pendingNum = new AtomicLong(0);
        this.cancels = PlatformDependent.newMpscQueue();
        this.timeouts = PlatformDependent.newMpscQueue();
        this.unProcess = PlatformDependent.newMpscQueue();
        this.tickDuration = fitTickDuration(tickDuration);
        this.workerState = new AtomicInteger(WORKER_INIT);
    }

    public void startup() {
        if (!set(WORKER_INIT, WORKER_STARTED)) {return;}
        Thread t = new Thread(this::doWheel);
        t.setName("wheel-timer-thread");
        t.setDaemon(false);
        t.start();
        this.workerThread = t;
    }

    public Queue<TimerTask> shutdown() {
        if (Thread.currentThread() == workerThread) {
            throw new RuntimeException();
        }
        if (!set(WORKER_STARTED, WORKER_SHUTDOWN)) {
            return null;
        }
        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return unProcess;
    }

    private long fitTickDuration(long tickDuration) {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long duration = unit.toNanos(tickDuration);
        return duration < 1 ? 1 : duration;
    }

    private TimerBucket[] createBucket(int ticksPerWheel) {
        ticksPerWheel = MathUtil.adjust2pow(ticksPerWheel);
        TimerBucket[] wheel = new TimerBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new TimerBucket(this);
        }
        return wheel;
    }

    private void processCancels() {
        for (; ; ) {
            TimerTask task = cancels.poll();
            if (task == null) {break;}
            task.remove();
        }
    }

    private long waitNextTick(long deadline) {
        long curGmt = System.nanoTime() - startTime;
        long sleepMs = (deadline - curGmt + 999999) / 1000000;
        if (sleepMs <= 0) {
            return curGmt == Long.MIN_VALUE ? -Long.MAX_VALUE : curGmt;
        }
        if (PlatformDependent.isWindows()) {
            sleepMs = sleepMs / 10 * 10;
            sleepMs = sleepMs == 0 ? 1 : sleepMs;
        }
        OtherUtil.sleep(sleepMs);
        return curGmt;
    }

    private void collectUnProcess() {
        for (TimerBucket bucket : buckets) {
            bucket.clearTimeouts(unProcess);
        }
        for (; ; ) {
            TimerTask timeout = timeouts.poll();
            if (timeout == null) {break;}
            if (!timeout.isCancelled()) {
                unProcess.add(timeout);
            }
        }
    }

    private boolean moveNode(TimerTask task) {
        if (task == null) {return false;}
        if (task.state() == ST_CANCELLED) {return true;}
        long calculated = task.getDeadline() / tickDuration;
        task.setRemaining((calculated - curTick) / buckets.length);
        final long ticks = Math.max(calculated, curTick);
        int stopIndex = (int)(ticks & (buckets.length - 1));
        TimerBucket bucket = buckets[stopIndex];
        task.setBucket(bucket);
        bucket.addTimeout(task);
        return true;
    }

    private void doWheel() {
        boolean status = false;
        startTime = System.nanoTime();
        startTime = startTime == 0 ? 1 : startTime;
        while (!Thread.interrupted()) {
            long expire = tickDuration * (curTick + 1);
            long deadline = waitNextTick(expire);
            if (deadline <= 0) {continue;}
            int idx = (int)(curTick & (buckets.length - 1));
            processCancels();
            TimerBucket bucket = buckets[idx];
            status = true;
            for (int i = 0; status && i < 10000; i++) {
                status = moveNode(timeouts.poll());
            }
            bucket.expireTimeouts(deadline);
            curTick++;
        }
        collectUnProcess();
        processCancels();
    }

    public Queue<TimerTask> getCancels() {
        return cancels;
    }

    public void addCancel(TimerTask task) {
        cancels.add(task);
    }

    public void addTimeouts(TimerTask task) {
        timeouts.add(task);
    }

    public Queue<TimerTask> getTimeouts() {
        return timeouts;
    }

    public AtomicLong getPending() {
        return pendingNum;
    }

    public void addPending() {
        pendingNum.incrementAndGet();
    }

    public void decPending() {
        pendingNum.decrementAndGet();
    }

    public boolean addTimeout(TimerTask task, long delay) {
        long next = pendingNum.get() + 1;
        if (maxPending > 0 && next > maxPending) {return false;}
        pendingNum.incrementAndGet();
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long total = System.nanoTime() + unit.toNanos(delay);
        long deadline = total - startTime;
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        task.setDeadline(deadline);
        timeouts.add(task);
        return true;
    }

    private boolean set(int o, int n) {
        return workerState.compareAndSet(o, n);
    }

}
