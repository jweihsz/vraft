package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 5:59 下午
 */
public enum SerializerEnum {

    KRYO_ID((byte)0x1),

    FURY_ID((byte)0x2),

    HESSIAN_ID((byte)0x3),

    MAX_ID((byte)0x7F);

    private final byte type;

    SerializerEnum(byte type) {this.type = type;}

    public byte getType() {return type;}

}
