package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 3:14 下午
 */
public interface Serializer {
    
    default byte getTypeId() {return 0x00;}

    default byte[] serialize(Object obj) throws Exception {return null;}

    default <T> T deserialize(byte[] bytes, Class<T> cls) throws Exception {return null;}
}
