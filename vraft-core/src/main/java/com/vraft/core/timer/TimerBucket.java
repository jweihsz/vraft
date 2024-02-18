package com.vraft.core.timer;

import java.util.Queue;

/**
 * @author jweihsz
 * @version 2024/2/7 21:11
 **/
public class TimerBucket {
    private TimerTask head, tail;
    private final TimerWheel wheel;

    public TimerBucket(TimerWheel wheel) {
        this.wheel = wheel;
    }

    public void addTimeout(TimerTask task) {
        if (head == null) {
            head = tail = task;
        } else {
            tail.setNext(task);
            task.setPrev(tail);
            tail = task;
        }
    }

    public void expireTimeouts(long deadline) {
        TimerTask task = head;
        while (task != null) {
            TimerTask next = task.getNext();
            if (task.getRemaining() <= 0) {
                next = remove(task);
                if (task.getDeadline() <= deadline) {
                    task.expire();
                }
            } else if (task.isCancelled()) {
                next = remove(task);
            } else {
                task.setRemaining(task.getRemaining() - 1);
            }
            task = next;
        }
    }

    public TimerTask remove(TimerTask task) {
        TimerTask next = task.getNext();
        if (task.getPrev() != null) {
            task.getPrev().setNext(next);
        }
        if (task.getNext() != null) {
            task.getNext().setPrev(task.getPrev());
        }
        if (task == head) {
            if (task == tail) {
                tail = null;
                head = null;
            } else {
                head = next;
            }
        } else if (task == tail) {
            tail = task.getPrev();
        }
        wheel.decPending();
        return next;
    }

    public void clearTimeouts(Queue<TimerTask> unProcess) {
        TimerTask task = null;
        for (; ; ) {
            if ((task = pollTimeout()) == null) {
                return;
            }
            if (task.isExpired() || task.isCancelled()) {
                continue;
            }
            unProcess.add(task);
        }
    }

    private TimerTask pollTimeout() {
        TimerTask head = this.head;
        if (head == null) {
            return null;
        }
        TimerTask next = head.getNext();
        if (next == null) {
            tail = this.head = null;
        } else {
            this.head = next;
            next.setPrev(null);
        }
        head.setNext(null);
        head.setPrev(null);
        head.setBucket(null);
        return head;
    }
}
