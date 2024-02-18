package com.vraft.core.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.vraft.core.utils.SystemUtil;

/**
 * @author jweihsz
 * @version 2024/2/18 19:39
 **/
public class ThreadPool {
    private ThreadPool() {}

    private final static int keepAlive = 30;
    public final static ThreadPoolExecutor ACTOR;
    private final static int cpuNum = SystemUtil.getPhyCpuNum();

    static {
        ACTOR = new ThreadPoolExecutor(cpuNum, cpuNum, keepAlive,
            TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    }

}
