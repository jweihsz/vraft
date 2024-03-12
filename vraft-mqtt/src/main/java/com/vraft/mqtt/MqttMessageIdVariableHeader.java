package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:37
 **/
@Data
public class MqttMessageIdVariableHeader {
    private int messageId;
    private byte reasonCode;
    private MqttProperties properties;

    public MqttMessageIdVariableHeader() {}

    public MqttMessageIdVariableHeader(int messageId) {
        this.messageId = messageId;
        this.properties = null;
    }

    public MqttMessageIdVariableHeader(int messageId,
        MqttProperties properties) {
        this.messageId = messageId;
        this.properties = properties;
    }

    public int messageId() {return messageId;}

    public static MqttMessageIdVariableHeader from(int messageId) {
        if (messageId < 1 || messageId > 0xffff) {
            String errMsg = "messageId: " + messageId + " (expected: 1 ~ 65535)";
            throw new IllegalArgumentException(errMsg);
        }
        return new MqttMessageIdVariableHeader(messageId);
    }

    public static MqttMessageIdVariableHeader from(int messageId, MqttProperties properties) {
        if (messageId < 1 || messageId > 0xffff) {
            String errMsg = "messageId: " + messageId + " (expected: 1 ~ 65535)";
            throw new IllegalArgumentException(errMsg);
        }
        return new MqttMessageIdVariableHeader(messageId, properties);
    }

    public MqttMessageIdVariableHeader withEmptyProperties(int messageId) {
        return new MqttMessageIdVariableHeader(messageId, MqttProperties.NO_PROPERTIES);
    }
}
