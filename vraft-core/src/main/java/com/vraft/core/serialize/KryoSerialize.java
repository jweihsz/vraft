package com.vraft.core.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private final int bSize = 524288; //512KB
    private final Set<Type> rs = new HashSet<>();
    private static final ThreadLocal<Kryo> TL = new ThreadLocal<>();
    private static final ThreadLocal<Input> I = new ThreadLocal<>();
    private static final ThreadLocal<Output> O = new ThreadLocal<>();

    public KryoSerialize() {}

    @Override
    public void registerClz(List<Type> clz) {
        if (clz == null || clz.isEmpty()) {return;}
        rs.addAll(clz);
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bao = null;
        bao = new ByteArrayOutputStream();
        Output output = getOutput();
        output.setOutputStream(bao);
        getKryo().writeClassAndObject(output, obj);
        output.close();
        return bao.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> cls) throws Exception {
        ByteArrayInputStream bai = null;
        Kryo kryo = getKryo();
        bai = new ByteArrayInputStream(bytes);
        Input input = getInput();
        input.setInputStream(bai);
        input.close();
        return cls.cast(kryo.readClassAndObject(input));
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

    private Input getInput() {
        Input input = I.get();
        if (input != null) {return input;}
        input = new Input(bSize);
        I.set(input);
        return input;
    }

    private Output getOutput() {
        Output output = O.get();
        if (output != null) {return output;}
        output = new Output(bSize, -1);
        O.set(output);
        return output;
    }

}
