package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 2024/2/5 17:28
 */
public interface SerializerService {

    default Serializer getSerializer(SerializerEnum type) {
        return null;
    }
}
