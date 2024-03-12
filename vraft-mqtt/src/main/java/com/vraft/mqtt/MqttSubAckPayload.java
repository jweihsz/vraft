package com.vraft.mqtt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vraft.mqtt.MqttReasonCodes.SubAck;
import io.netty.util.internal.StringUtil;

/**
 * @author jweihsz
 * @version 2024/3/11 21:03
 **/
public class MqttSubAckPayload {
    private List<SubAck> reasonCodes;
    private int numberOfBytesConsumed;

    public MqttSubAckPayload() {
        this.reasonCodes = new ArrayList<>();
    }

    public void add(int... codes) {
        for (int v : codes) {
            SubAck a = MqttReasonCodes.SubAck.valueOf((byte)(v & 0xFF));
            reasonCodes.add(a);
        }
    }

    public void recycle() {
        reasonCodes.clear();
    }

    public MqttSubAckPayload(int... reasonCodes) {
        List<MqttReasonCodes.SubAck> list = new ArrayList<SubAck>(reasonCodes.length);
        for (int v : reasonCodes) {
            list.add(MqttReasonCodes.SubAck.valueOf((byte)(v & 0xFF)));
        }
        this.reasonCodes = Collections.unmodifiableList(list);
    }

    public MqttSubAckPayload(MqttReasonCodes.SubAck... reasonCodes) {
        List<MqttReasonCodes.SubAck> list = new ArrayList<MqttReasonCodes.SubAck>(reasonCodes.length);
        list.addAll(Arrays.asList(reasonCodes));
        this.reasonCodes = Collections.unmodifiableList(list);
    }

    public MqttSubAckPayload(Iterable<Integer> reasonCodes) {
        List<MqttReasonCodes.SubAck> list = new ArrayList<MqttReasonCodes.SubAck>();
        for (Integer v : reasonCodes) {
            if (v == null) {break;}
            list.add(MqttReasonCodes.SubAck.valueOf(v.byteValue()));
        }
        this.reasonCodes = Collections.unmodifiableList(list);
    }

    public List<Integer> grantedQoSLevels() {
        List<Integer> qosLevels = new ArrayList<Integer>(reasonCodes.size());
        for (MqttReasonCodes.SubAck code : reasonCodes) {
            if ((code.byteValue() & 0xFF) > MqttQoS.EXACTLY_ONCE.value()) {
                qosLevels.add(MqttQoS.FAILURE.value());
            } else {
                qosLevels.add(code.byteValue() & 0xFF);
            }
        }
        return qosLevels;
    }

    public List<Integer> reasonCodes() {
        return typedReasonCodesToOrdinal();
    }

    private List<Integer> typedReasonCodesToOrdinal() {
        List<Integer> intCodes = new ArrayList<Integer>(reasonCodes.size());
        for (MqttReasonCodes.SubAck code : reasonCodes) {
            intCodes.add(code.byteValue() & 0xFF);
        }
        return intCodes;
    }

    public List<MqttReasonCodes.SubAck> typedReasonCodes() {
        return reasonCodes;
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
            .append('[')
            .append("reasonCodes=").append(reasonCodes)
            .append(']')
            .toString();
    }
}
