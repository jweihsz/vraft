package com.vraft.core.timer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

import com.vraft.common.MathUtil;

import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.PlatformDependent;

/**
 * @author jweihsz
 * @version 2024/2/7 20:55
 **/
public class TimerWheel {

    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;
    private final AtomicLong pendingNum;
    private final Queue<TimerTask> cancels;
    private final Queue<TimerTask> timeouts;
    private final TimerBucket[] buckets;
    private final int mask;
    private volatile long startTime;
    private final Thread workerThread;
    private final long maxPendingTimeouts;
    private final long tickDuration;
    private final Worker worker = new Worker();
    private volatile int workerState; // 0 - init, 1 - started, 2 - shut down
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
    private static final AtomicIntegerFieldUpdater<TimerWheel> WORKER_STATE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(TimerWheel.class, "workerState");

    public TimerWheel(long tickDuration, TimeUnit unit, int ticksPerWheel, boolean leakDetection,
        long maxPendingTimeouts) {
        this.pendingNum = new AtomicLong(0);
        this.cancels = PlatformDependent.newMpscQueue();
        this.timeouts = PlatformDependent.newMpscQueue();
        buckets = createWheel(ticksPerWheel);
        mask = buckets.length - 1;
        long duration = unit.toNanos(tickDuration);
        if (duration < 1) {
            this.tickDuration = 1;
        } else {
            this.tickDuration = duration;
        }
        workerThread = new Thread(worker);
        this.maxPendingTimeouts = maxPendingTimeouts;
    }

    private TimerBucket[] createWheel(int ticksPerWheel) {
        ticksPerWheel = MathUtil.adjust2pow(ticksPerWheel);
        TimerBucket[] wheel = new TimerBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new TimerBucket();
        }
        return wheel;
    }

    public Queue<TimerTask> getCancels() {
        return cancels;
    }

    public Queue<TimerTask> getTimeouts() {
        return timeouts;
    }

    public AtomicLong getPending() {
        return pendingNum;
    }

    public long addPending() {
        return pendingNum.incrementAndGet();
    }

    public long decPending() {
        return pendingNum.decrementAndGet();
    }

    public void start() {
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }

        // Wait until the startTime is initialized by the worker.
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - it will be ready very soon.
            }
        }
    }

    public TimerTask newTimeout(TimerTask task, long delay, TimeUnit unit) {

        long pendingTimeoutsCount = pendingNum.incrementAndGet();

        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingNum.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts (" + pendingTimeoutsCount
                + ") is greater than or equal to maximum allowed pending " + "timeouts (" + maxPendingTimeouts + ")");
        }
        start();
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        task.deadline = deadline;
        timeouts.add(task);
        return task;
    }

    public Set<TimerTask> stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(HashedWheelTimer.class.getSimpleName() + ".stop() cannot be called from "
                + io.netty.util.TimerTask.class.getSimpleName());
        }

        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // workerState can be 0 or 2 at this moment - let it always be 2.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {

            }

            return Collections.emptySet();
        }

        try {
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
        } finally {

        }
        return worker.unprocessedTimeouts();
    }

    private final class Worker implements Runnable {
        private final Set<TimerTask> unprocessedTimeouts = new HashSet<>();

        private long tick;

        @Override
        public void run() {
            startTime = System.nanoTime();
            if (startTime == 0) {
                startTime = 1;
            }
            startTimeInitialized.countDown();

            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    int idx = (int)(tick & mask);
                    processCancelledTasks();
                    TimerBucket bucket = buckets[idx];
                    transferTimeoutsToBuckets();
                    bucket.expireTimeouts(deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(TimerWheel.this) == WORKER_STATE_STARTED);

            for (TimerBucket bucket : buckets) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            for (;;) {
                TimerTask timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            processCancelledTasks();
        }

        private void transferTimeoutsToBuckets() {

            for (int i = 0; i < 100000; i++) {
                TimerTask timeout = timeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                if (timeout.state() == TimerTask.ST_CANCELLED) {
                    continue;
                }

                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / buckets.length;

                final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
                int stopIndex = (int)(ticks & mask);

                TimerBucket bucket = buckets[stopIndex];
                bucket.addTimeout(timeout);
            }
        }

        private void processCancelledTasks() {
            for (;;) {
                TimerTask timeout = cancels.poll();
                if (timeout == null) {
                    break;
                }
                try {
                    timeout.remove();
                } catch (Throwable t) {

                }
            }
        }

        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);

            for (;;) {
                final long currentTime = System.nanoTime() - startTime;
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }
                if (PlatformDependent.isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                    if (sleepTimeMs == 0) {
                        sleepTimeMs = 1;
                    }
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (WORKER_STATE_UPDATER.get(TimerWheel.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        public Set<TimerTask> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

}
