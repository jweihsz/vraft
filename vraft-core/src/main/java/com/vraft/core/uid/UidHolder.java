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

    private final IdGenerator genRpcId;
    private final IdGenerator genUserId;
    private final IdGenerator genActorId;

    public UidHolder() {
        this.genRpcId = new SeqUid();
        this.genUserId = new SeqUid();
        this.genActorId = new SeqUid();
    }

    @Override
    public long genUserId() {
        return genUserId.nextId();
    }

    @Override
    public long genMsgId() {
        return genRpcId.nextId();
    }

    @Override
    public long genActorId() {
        return genActorId.nextId();
    }
}
