package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 2024/2/5 17:59
 */
public enum SerializerEnum {

    KRYO_ID((byte)0x1),

    FURY_ID((byte)0x2),

    HESSIAN_ID((byte)0x3),

    MAX_ID((byte)0x7F);

    private final byte type;

    SerializerEnum(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

}
