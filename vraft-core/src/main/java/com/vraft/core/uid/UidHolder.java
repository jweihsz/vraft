package com.vraft.core.uid;

import com.vraft.facade.uid.IdGenerator;
import com.vraft.facade.uid.UidService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/7 15:46
 **/
public class UidHolder implements UidService {
    private final static Logger logger = LogManager.getLogger(UidHolder.class);

    private final IdGenerator rpcIdGenerator;

    public UidHolder() {
        this.rpcIdGenerator = new SeqUid(1L);
    }

    @Override
    public IdGenerator getRpcIdGenerator() {
        return rpcIdGenerator;
    }
}
