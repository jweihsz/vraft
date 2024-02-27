package com.vraft.facade.serializer;

/**
 * @author jweih.hjw
 * @version 2024/2/5 17:28
 */
public interface SerializerMgr {

    Serializer newKryo();

    Serializer newFury();

    Serializer newHessian();

    Serializer get(SerializerEnum type);
}
