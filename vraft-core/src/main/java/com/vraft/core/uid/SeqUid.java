package com.vraft.core.uid;

import java.util.concurrent.atomic.AtomicLong;

import com.vraft.facade.uid.IdGenerator;

/**
 * @author jweihsz
 * @version 2024/2/7 14:48
 **/
public class SeqUid implements IdGenerator {

    private final AtomicLong id;

    public SeqUid() {
        this.id = new AtomicLong(1L);
    }
    
    public SeqUid(long val) {
        this.id = new AtomicLong(val);
    }

    @Override
    public long nextId() {
        return id.incrementAndGet();
    }

    @Override
    public void restId(long val) {
        id.set(val);
    }
}
