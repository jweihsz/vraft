package com.vraft.core.actor.base;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jweihsz
 * @version 2024/2/8 02:31
 * @mark copy from qmq project
 **/
public class ActorSystem {
    private static final Logger logger = LoggerFactory.getLogger(ActorSystem.class);

    private final ThreadPoolExecutor executor;
    private final AtomicInteger actorsCount;
    private final ConcurrentMap<Long, Actor> actors;

    public ActorSystem(ThreadPoolExecutor executor) {
        this.executor = executor;
        this.actorsCount = new AtomicInteger();
        this.actors = new ConcurrentHashMap<>();
    }

    public <E> void dispatch(long actorId, E msg, Processor<E> processor) {
        Actor<E> actor = createOrGet(actorId, processor);
        actor.dispatch(msg);
        schedule(actor, true);
    }

    public void suspend(long actorId) {
        Actor actor = actors.get(actorId);
        if (actor == null) {return;}
        actor.suspend();
    }

    public void resume(long actorId) {
        Actor actor = actors.get(actorId);
        if (actor == null) {return;}
        actor.resume();
        schedule(actor, false);
    }

    public <E> Actor<E> createOrGet(long actorId, Processor<E> processor) {
        Actor<E> actor = actors.get(actorId);
        if (actor != null) {return actor;}
        Actor<E> add = new Actor<>(actorId, this, processor);
        Actor<E> old = actors.putIfAbsent(actorId, add);
        if (old == null) {
            actorsCount.incrementAndGet();
            return add;
        } else {
            return old;
        }
    }

    private <E> boolean schedule(Actor<E> actor, boolean hasMessageHint) {
        if (!actor.canBeSchedule(hasMessageHint)) { return false; }
        if (actor.setAsScheduled()) {
            actor.submitTs = System.currentTimeMillis();
            this.executor.execute(actor);
            return true;
        }
        return false;
    }

    public interface Processor<T> {
        boolean process(long deadline, Actor<T> self);
    }

    public static class Actor<E> implements Runnable, Comparable<Actor> {
        private static final int Open = 0;
        private static final int Scheduled = 2;
        private static final int shouldScheduleMask = 3;
        private static final int shouldNotProcessMask = ~2;
        private static final int suspendUnit = 4;
        private static final int QUOTA = 5;
        private static long statusOffset;

        static {
            try {
                statusOffset = Unsafe.instance.objectFieldOffset(Actor.class.getDeclaredField("status"));
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
        }

        final ActorSystem actorSystem;
        final Queue<E> queue;
        final Processor<E> processor;
        private long total, actorId;
        private volatile long submitTs;
        private volatile long executeTs;
        private volatile int status;

        Actor(long actorId, ActorSystem actorSystem, Processor<E> processor) {
            this.actorSystem = actorSystem;
            this.processor = processor;
            this.actorId = actorId;
            this.queue = PlatformDependent.newMpscQueue();
        }

        boolean dispatch(E message) {
            return queue.add(message);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            executeTs = start;
            try {
                if (shouldProcessMessage()) {
                    long deadline = System.currentTimeMillis() + QUOTA;
                    processor.process(deadline, this);
                }
            } finally {
                long duration = System.currentTimeMillis() - start;
                total += duration;
                setAsIdle();
                this.actorSystem.schedule(this, false);
            }
        }

        final boolean shouldProcessMessage() {
            return (currentStatus() & shouldNotProcessMask) == 0;
        }

        private boolean canBeSchedule(boolean hasMessageHint) {
            int s = currentStatus();
            if (s == Open || s == Scheduled) { return hasMessageHint || !queue.isEmpty(); }
            return false;
        }

        public final boolean resume() {
            while (true) {
                int s = currentStatus();
                int next = s < suspendUnit ? s : s - suspendUnit;
                if (updateStatus(s, next)) { return next < suspendUnit; }
            }
        }

        public final void suspend() {
            while (true) {
                int s = currentStatus();
                if (updateStatus(s, s + suspendUnit)) { return; }
            }
        }

        final boolean setAsScheduled() {
            while (true) {
                int s = currentStatus();
                if ((s & shouldScheduleMask) != Open) { return false; }
                if (updateStatus(s, s | Scheduled)) { return true; }
            }
        }

        final void setAsIdle() {
            while (true) {
                int s = currentStatus();
                if (updateStatus(s, s & ~Scheduled)) { return; }
            }
        }

        final int currentStatus() {
            return Unsafe.instance.getIntVolatile(this, statusOffset);
        }

        private boolean updateStatus(int oldStatus, int newStatus) {
            return Unsafe.instance.compareAndSwapInt(this, statusOffset, oldStatus, newStatus);
        }

        @Override
        public int compareTo(Actor o) {
            return ActorCompareWay.LastExecuteTimestamp.compare(this, o);
        }
    }

    private enum ActorCompareWay {

        LastExecuteTimestamp {
            @Override
            public int compare(Actor a1, Actor a2) {
                int result = Long.compare(a1.executeTs, a2.executeTs);
                return result == 0 ? Long.compare(a1.total, a2.total) : result;
            }
        },
        TotalDuration {
            @Override
            public int compare(Actor a1, Actor a2) {
                int result = Long.compare(a1.total, a2.total);
                return result == 0 ? Long.compare(a1.submitTs, a2.submitTs) : result;
            }
        };

        public abstract int compare(Actor a1, Actor a2);
    }

    static class Unsafe {
        public final static sun.misc.Unsafe instance;

        static {
            try {
                sun.misc.Unsafe found = null;
                for (Field field : sun.misc.Unsafe.class.getDeclaredFields()) {
                    if (field.getType() == sun.misc.Unsafe.class) {
                        field.setAccessible(true);
                        found = (sun.misc.Unsafe)field.get(null);
                        break;
                    }
                }
                if (found == null) { throw new IllegalStateException("Can't find instance of sun.misc.Unsafe"); } else {
                    instance = found;
                }
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
        }
    }
}
