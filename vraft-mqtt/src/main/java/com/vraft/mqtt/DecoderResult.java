package com.vraft.mqtt;

import io.netty.util.Signal;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:14
 **/
@Data
public class DecoderResult {
    protected static final Signal SIGNAL_UNFINISHED = Signal.valueOf(DecoderResult.class, "UNFINISHED");
    protected static final Signal SIGNAL_SUCCESS = Signal.valueOf(DecoderResult.class, "SUCCESS");

    public static final DecoderResult UNFINISHED = new DecoderResult(SIGNAL_UNFINISHED);
    public static final DecoderResult SUCCESS = new DecoderResult(SIGNAL_SUCCESS);

    public static DecoderResult failure(Throwable cause) {
        return new DecoderResult(cause);
    }

    private Throwable cause;

    public DecoderResult(Throwable cause) {
        this.cause = cause;
    }

    public DecoderResult() {
        this.cause = null;
    }

    public void recycle() {
        this.cause = null;
    }

    public boolean isFinished() {
        return cause != SIGNAL_UNFINISHED;
    }

    public boolean isSuccess() {
        return cause == SIGNAL_SUCCESS;
    }

    public boolean isFailure() {
        return cause != SIGNAL_SUCCESS && cause != SIGNAL_UNFINISHED;
    }

    public Throwable cause() {
        return isFailure() ? cause : null;
    }
}

