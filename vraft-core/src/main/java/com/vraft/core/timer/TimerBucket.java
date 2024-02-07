package com.vraft.core.timer;

import java.util.Set;

/**
 * @author jweihsz
 * @version 2024/2/7 21:11
 **/
public class TimerBucket {
    private TimerTask head, tail;

    public void addTimeout(TimerTask task) {
        task.bucket = this;
        if (head == null) {
            head = tail = task;
        } else {
            tail.next = task;
            task.prev = tail;
            tail = task;
        }
    }

    public void expireTimeouts(long deadline) {
        TimerTask timeout = head;
        while (timeout != null) {
            TimerTask next = timeout.next;
            if (timeout.remainingRounds <= 0) {
                next = remove(timeout);
                if (timeout.deadline <= deadline) {
                    timeout.expire();
                }
            } else if (timeout.isCancelled()) {
                next = remove(timeout);
            } else {
                timeout.remainingRounds--;
            }
            timeout = next;
        }
    }

    public TimerTask remove(TimerTask timeout) {
        TimerTask next = timeout.next;
        if (timeout.prev != null) {
            timeout.prev.next = next;
        }
        if (timeout.next != null) {
            timeout.next.prev = timeout.prev;
        }

        if (timeout == head) {
            if (timeout == tail) {
                tail = null;
                head = null;
            } else {
                head = next;
            }
        } else if (timeout == tail) {
            tail = timeout.prev;
        }
        timeout.prev = null;
        timeout.next = null;
        timeout.bucket = null;
        timeout.wheel.decPending();
        return next;
    }

    public void clearTimeouts(Set<TimerTask> set) {
        for (;;) {
            TimerTask timeout = pollTimeout();
            if (timeout == null) {
                return;
            }
            if (timeout.isExpired() || timeout.isCancelled()) {
                continue;
            }
            set.add(timeout);
        }
    }

    private TimerTask pollTimeout() {
        TimerTask head = this.head;
        if (head == null) {
            return null;
        }
        TimerTask next = head.next;
        if (next == null) {
            tail = this.head = null;
        } else {
            this.head = next;
            next.prev = null;
        }
        head.next = null;
        head.prev = null;
        head.bucket = null;
        return head;
    }
}
