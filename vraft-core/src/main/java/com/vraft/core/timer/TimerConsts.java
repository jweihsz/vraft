package com.vraft.core.timer;

/**
 * @author jweihsz
 * @version 2024/2/7 23:47
 **/
public class TimerConsts {
    public static final int ST_INIT = 0;
    public static final int ST_EXPIRED = 2;
    public static final int ST_CANCELLED = 1;
    public static final int WORKER_INIT = 0;
    public static final int WORKER_STARTED = 1;
    public static final int WORKER_SHUTDOWN = 2;
}
