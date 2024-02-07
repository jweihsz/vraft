package com.vraft.core.uid;

import java.util.concurrent.atomic.AtomicLong;

import com.vraft.common.OtherUtil;
import com.vraft.facade.uid.IdGenerator;

/**
 * @author jweihsz
 * @version 2024/2/7 14:51 |flag(1)|nodeId(10b)|timestamp(41b)|seq(12b)|
 **/
public class Snowflake implements IdGenerator {

    private final long epoch;
    private final long nodeId;
    private final int seqBits;
    private final int nodeBits;
    private final int timeBits;
    private final long nextMask;
    private final AtomicLong nextId;

    public Snowflake(long nodeId) {
        this.seqBits = 12;
        this.timeBits = 41;
        this.nodeBits = 10;
        this.epoch = 1707288988000L;
        this.nodeId = getNodeId(nodeId);
        this.nextId = initNextId();
        this.nextMask = ~(-1L << (timeBits + seqBits));
    }

    @Override
    public long nextId() {
        waitNecessary();
        return nodeId | (nextId.incrementAndGet() & nextMask);
    }

    @Override
    public void restId(long val) {}

    private void waitNecessary() {
        long cur = nextId.get() >>> seqBits;
        long now = System.currentTimeMillis() - epoch;
        while (cur >= now) {
            OtherUtil.sleep(5);
            now = System.currentTimeMillis() - epoch;
        }
    }

    private long getNodeId(long nodeId) {
        int max = ~(-1 << nodeBits);
        if (nodeId < 0 || nodeId > max) {
            throw new RuntimeException();
        }
        return nodeId << (timeBits + seqBits);
    }

    private AtomicLong initNextId() {
        long cur = System.currentTimeMillis();
        long val = (cur - epoch) << seqBits;
        return new AtomicLong(val);
    }

}
