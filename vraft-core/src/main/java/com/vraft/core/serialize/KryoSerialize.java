package com.vraft.core.serialize;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 2024/2/5 16:32
 */
public class KryoSerialize implements Serializer {
    private final static Logger logger = LogManager.getLogger(KryoSerialize.class);

    private final Set<Type> rs = new HashSet<>();
    private static final ThreadLocal<Kryo> TL = new ThreadLocal<>();

    public KryoSerialize() {}

    @Override
    public void registerClz(List<Type> clz) {
        if (clz == null || clz.isEmpty()) {return;}
        rs.addAll(clz);
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        Output output = new Output(256, -1);
        getKryo().writeObject(output, obj);
        output.flush();
        byte[] bytes = output.toBytes();
        output.close();
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> cls) throws Exception {
        Input input = new Input(bytes);
        T obj = getKryo().readObject(input, cls);
        input.close();
        return obj;
    }

    @Override
    public byte getTypeId() {
        return SerializerEnum.KRYO_ID.getType();
    }

    private void register(Kryo kryo, Set<Type> rs) {
        if (rs == null || rs.isEmpty()) {return;}
        for (Type cls : rs) {kryo.register((Class)cls);}
    }

    private Kryo getKryo() {
        Kryo kryo = TL.get();
        if (kryo != null) {return kryo;}
        kryo = new Kryo();
        register(kryo, rs);
        TL.set(kryo);
        return kryo;
    }

}
