package com.vraft.facade.uid;

/**
 * @author jweihsz
 * @version 2024/2/7 14:45
 **/
public interface IdGenerator {

    long nextId();

    void restId(long val);

}
