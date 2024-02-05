package com.vraft.facade.serializer;

import java.lang.reflect.Type;
import java.util.List;

import lombok.Data;

/**
 * @author jweih.hjw
 * @version 2024/2/5 17:36
 */
@Data
public class SerializeOpt {
    private boolean isHessian;

    private boolean isKryo;
    private List<Type> kryoCls;

    private boolean isFury;
    private List<Type> furyCls;

}
