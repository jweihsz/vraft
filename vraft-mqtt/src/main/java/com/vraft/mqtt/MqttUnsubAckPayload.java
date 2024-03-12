package com.vraft.mqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vraft.mqtt.MqttReasonCodes.UnsubAck;
import io.netty.util.internal.StringUtil;

/**
 * @author jweihsz
 * @version 2024/3/11 21:11
 **/
public final class MqttUnsubAckPayload {
    private int numberOfBytesConsumed;
    private List<UnsubAck> unsubscribeReasonCodes;

    private static final MqttUnsubAckPayload EMPTY = new MqttUnsubAckPayload();

    public MqttUnsubAckPayload() {
        this.unsubscribeReasonCodes = new ArrayList<>();
    }

    public void recycle() {
        this.unsubscribeReasonCodes.clear();
    }

    public void add(short... codes) {
        for (short v : codes) {
            unsubscribeReasonCodes.add(MqttReasonCodes.UnsubAck.valueOf((byte)(v & 0xFF)));
        }
    }

    public static MqttUnsubAckPayload withEmptyDefaults(MqttUnsubAckPayload payload) {
        return payload == null ? EMPTY : payload;
    }

    public MqttUnsubAckPayload(short... unsubscribeReasonCodes) {
        List<MqttReasonCodes.UnsubAck> list = new ArrayList<UnsubAck>(unsubscribeReasonCodes.length);
        for (Short v : unsubscribeReasonCodes) {
            list.add(MqttReasonCodes.UnsubAck.valueOf((byte)(v & 0xFF)));
        }
        this.unsubscribeReasonCodes = Collections.unmodifiableList(list);
    }

    public MqttUnsubAckPayload(Iterable<Short> unsubscribeReasonCodes) {
        List<MqttReasonCodes.UnsubAck> list = new ArrayList<MqttReasonCodes.UnsubAck>();
        for (Short v : unsubscribeReasonCodes) {
            list.add(MqttReasonCodes.UnsubAck.valueOf(v.byteValue()));
        }
        this.unsubscribeReasonCodes = Collections.unmodifiableList(list);
    }

    public List<Short> unsubscribeReasonCodes() {
        return typedReasonCodesToOrdinal();
    }

    private List<Short> typedReasonCodesToOrdinal() {
        List<Short> codes = new ArrayList<Short>(unsubscribeReasonCodes.size());
        for (MqttReasonCodes.UnsubAck code : unsubscribeReasonCodes) {
            codes.add((short)(code.byteValue() & 0xFF));
        }
        return codes;
    }

    public List<MqttReasonCodes.UnsubAck> typedReasonCodes() {
        return unsubscribeReasonCodes;
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
            .append('[')
            .append("unsubscribeReasonCodes=").append(unsubscribeReasonCodes)
            .append(']')
            .toString();
    }
}