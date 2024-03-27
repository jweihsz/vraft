package com.vraft.core.actor;

import java.util.ArrayList;
import java.util.List;
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

    public <E> boolean dispatch(long extId, long subId,
        E msg, ActorProcessor<E> processor) {
        Actor<E> actor = createOrGet(extId, subId, processor);
        boolean status = actor.dispatch(msg);
        schedule(actor, true);
        return status;
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

    public <E> Actor<E> createOrGet(
        long extId, long subId,
        ActorProcessor<E> processor) {
        long actorId = processor.actorId(extId, subId);
        Actor<E> actor = actors.get(actorId);
        if (actor != null) {return actor;}
        Actor<E> add = new Actor<>(actorId, extId, subId,
            this, processor);
        Actor<E> old = actors.putIfAbsent(actorId, add);
        if (old != null) {return old;}
        actorsCount.incrementAndGet();
        return add;
    }

    private <E> boolean schedule(Actor<E> actor, boolean hasMessageHint) {
        if (!actor.canBeSchedule(hasMessageHint)) {return false;}
        if (!actor.setAsScheduled()) {return false;}
        actor.submitTs = System.currentTimeMillis();
        this.executor.execute(actor);
        return true;
    }

    public interface ActorProcessor<T> {
        long actorId(long extId, long subId);

        void process(long deadline, Actor<T> self);
    }

    public static class Actor<E> implements Runnable, Comparable<Actor> {
        private static final int QUOTA = 5;
        private static final int suspendUnit = 4;
        private static final int Open = 0;
        private static final int Scheduled = 2;
        private static final int shouldScheduleMask = 3;
        private static final int shouldNotProcessMask = ~2;
        private final long extId;
        private final long subId;
        private final Queue<E> queue;
        private AtomicInteger status;
        private long total;
        private long actorId;
        private volatile long submitTs;
        private volatile long executeTs;
        private final List<E> dataList;
        final ActorSystem actorSystem;
        final ActorProcessor<E> processor;

        public Actor(long actorId, long extId,
            long subId, ActorSystem actorSystem,
            ActorProcessor<E> processor) {
            this.extId = extId;
            this.subId = subId;
            this.actorId = actorId;
            this.status = new AtomicInteger();
            this.actorSystem = actorSystem;
            this.processor = processor;
            this.dataList = new ArrayList<>();
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
            return (s == Open || s == Scheduled)
                && (hasMessageHint || !queue.isEmpty());

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

        public Queue<E> getQueue() {return queue;}

        public List<E> getDataList() {return dataList;}

        public long getExtId() {return extId;}

        public long getSubId() {return subId;}

        public long getActorId() {return actorId;}

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

        final int currentStatus() {return status.get();}

        private boolean updateStatus(int oldStatus, int newStatus) {
            return status.compareAndSet(oldStatus, newStatus);
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

}
