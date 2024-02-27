package com.vraft.core.serialize;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import io.fury.Fury;
import io.fury.config.FuryBuilder;
import io.fury.config.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 2024/2/5 17:05
 */
public class FurySerialize implements Serializer {
    private final static Logger logger = LogManager.getLogger(FurySerialize.class);

    private final Set<Type> rs = new HashSet<>();
    private static final ThreadLocal<Fury> TL = new ThreadLocal<>();

    public FurySerialize() {}

    @Override
    public void registerClz(List<Type> clz) {
        if (clz == null || clz.isEmpty()) {return;}
        rs.addAll(clz);
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        return getFury().serialize(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> cls) throws Exception {
        return getFury().deserializeJavaObject(bytes, cls);
    }

    @Override
    public byte getTypeId() {
        return SerializerEnum.FURY_ID.getType();
    }

    private void register(Fury fury, Set<Type> rs) {
        if (rs == null || rs.isEmpty()) {return;}
        for (Type cls : rs) {
            fury.register((Class)cls);
        }
    }

    private Fury getFury() {
        Fury fury = TL.get();
        if (fury != null) {
            return fury;
        }
        FuryBuilder fb = Fury.builder();
        fb.withLanguage(Language.JAVA);
        fb.requireClassRegistration(true);
        fury = fb.build();
        register(fury, rs);
        TL.set(fury);
        return fury;
    }
}