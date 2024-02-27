package com.vraft.core.serialize;

import javax.annotation.concurrent.NotThreadSafe;

import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 2024/2/5 15:53
 */
@NotThreadSafe
public class SerializeHolder implements SerializerMgr {
    private final static Logger logger = LogManager.getLogger(SerializeHolder.class);

    public SerializeHolder() {}

    @Override
    public Serializer newHessian() {
        return new HessianSerialize();
    }

    @Override
    public Serializer newKryo() {
        return new KryoSerialize();
    }

    @Override
    public Serializer newFury() {
        return new FurySerialize();
    }

}
