package com.vraft.test.timer;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.vraft.core.timer.TimerHolder;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jweihsz
 * @version 2024/2/29 14:55
 **/
public class TimerServerTest {
    private final static Logger logger = LogManager.getLogger(TimerServerTest.class);

    private SystemCtx sysCtx;
    private TimerService timer;

    @Before
    public void bf() throws Exception {
        sysCtx = new SystemCtx();
        timer = new TimerHolder(sysCtx);
        timer.startup();
    }

    @Test
    public void testTimer() throws Exception {
        CountDownLatch ct = new CountDownLatch(1);
        TimerParams params = new TimerParams("timer", 1);
        Consumer<Object> cb = buildConsumer();
        params.setCb(cb);
        Object task = timer.addTimeout(cb, params, 3000);
        if (task == null) {
            logger.error("add timer task fail");
        } else {
            logger.info("add timer task ok");
        }
        ct.await();
    }

    private Consumer<Object> buildConsumer() {
        return (params) -> {
            if (!(params instanceof TimerParams)) {return;}
            logger.info("params:{}", params);
            TimerParams n = (TimerParams)params;
            n.setId(n.getId() + 1);
            timer.addTimeout(n.getCb(), n, 3000);
        };
    }

    @Data
    private static class TimerParams {
        private long id;
        private String name;
        private transient Consumer<Object> cb;

        public TimerParams(String name, long id) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "id:" + id + ",name:" + name;
        }
    }

}
