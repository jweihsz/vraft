package com.vraft.facade.serializer;

import java.lang.reflect.Type;
import java.util.List;

import lombok.Data;

/**
 * @author jweih.hjw
 * @version 创建时间：2024/2/5 5:36 下午
 */
@Data
public class SerializeOpt {
    private boolean isHessian;
    
    private boolean isKryo;
    private List<Type> kryoCls;

    private boolean isFury;
    private List<Type> furyCls;

}
