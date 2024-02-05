package com.vraft.core.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.vraft.facade.serializer.Serializer;
import com.vraft.facade.serializer.SerializerEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 3:55 下午
 */
public class HessianSerialize implements Serializer {
    private final static Logger logger = LogManager.getLogger(HessianSerialize.class);

    private final SerializerFactory factory;
    private static final ThreadLocal<ByteArrayOutputStream> TL;

    static {
        TL = ThreadLocal.withInitial(ByteArrayOutputStream::new);
    }

    public HessianSerialize() {
        factory = new SerializerFactory();
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        Hessian2Output output = null;
        ByteArrayOutputStream byteArray = TL.get();
        byteArray.reset();
        output = new Hessian2Output(byteArray);
        output.setSerializerFactory(factory);
        output.writeObject(obj);
        output.close();
        return byteArray.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> cls) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Hessian2Input hi = new Hessian2Input(is);
        hi.setSerializerFactory(factory);
        Object obj = hi.readObject();
        hi.close();
        return cls.cast(obj);
    }

    @Override
    public byte getTypeId() {
        return SerializerEnum.HESSIAN_ID.getType();
    }
}
