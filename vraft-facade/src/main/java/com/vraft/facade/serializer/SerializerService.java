package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 5:28 下午
 */
public interface SerializerService {

    default Serializer getSerializer(SerializerEnum type) {return null;}
}
