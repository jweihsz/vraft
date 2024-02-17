package com.vraft.facade.timer;

import java.util.function.Consumer;

import com.vraft.facade.common.LifeCycle;

/**
 * @author jweihsz
 * @version 2024/2/8 02:28
 **/
public interface TimerService extends LifeCycle {

    default boolean removeTimeout(Object task) {return false;}

    default Object addTimeout(Consumer<Object> apply, Object param, long delay) {return null;}

}
