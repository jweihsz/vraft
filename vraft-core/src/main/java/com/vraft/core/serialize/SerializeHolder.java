package com.vraft.core.serialize;

import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vraft.facade.serializer.SerializeOpt;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import com.vraft.facade.serializer.SerializerService;

/**
 * @author jweih.hjw
 * @version 2024/2/5 15:53
 */
@NotThreadSafe
public class SerializeHolder implements SerializerService {
    private final static Logger logger = LogManager.getLogger(SerializeHolder.class);

    private final Serializer[] si;

    public SerializeHolder(SerializeOpt opt) {
        Objects.requireNonNull(opt);
        si = new Serializer[SerializerEnum.MAX_ID.getType() + 1];
        si[SerializerEnum.KRYO_ID.getType()] = newKryo(opt);
        si[SerializerEnum.FURY_ID.getType()] = newFury(opt);
        si[SerializerEnum.HESSIAN_ID.getType()] = newHessian(opt);
    }

    @Override
    public Serializer getSerializer(SerializerEnum type) {
        Serializer s = si[type.getType()];
        Objects.requireNonNull(s);
        return s;
    }

    private Serializer newHessian(SerializeOpt opt) {
        if (!opt.isHessian()) {
            return null;
        }
        return new HessianSerialize();
    }

    private Serializer newKryo(SerializeOpt opt) {
        if (!opt.isKryo()) {
            return null;
        }
        return new KryoSerialize(opt.getKryoCls());
    }

    private Serializer newFury(SerializeOpt opt) {
        if (!opt.isFury()) {
            return null;
        }
        return new FurySerialize(opt.getFuryCls());
    }

}
