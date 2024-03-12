package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 19:56
 **/
@Data
public class MqttFixedHeader {
    private boolean isDup;
    private MqttQoS qosLevel;
    private boolean isRetain;
    private int remainingLength;
    private MqttMessageType messageType;

    public MqttFixedHeader() {}

    public MqttFixedHeader(
        MqttMessageType messageType,
        boolean isDup,
        MqttQoS qosLevel,
        boolean isRetain,
        int remainingLength) {
        this.messageType = messageType;
        this.isDup = isDup;
        this.qosLevel = qosLevel;
        this.isRetain = isRetain;
        this.remainingLength = remainingLength;
    }

    public void recycle() {
        this.isDup = false;
        this.isRetain = false;
        this.remainingLength = -1;
        this.qosLevel = MqttQoS.FAILURE;
        this.messageType = MqttMessageType.AUTH;
    }
}
