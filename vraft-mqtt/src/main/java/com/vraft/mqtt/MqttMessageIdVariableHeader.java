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

    public MqttMessageIdVariableHeader() {
        this.properties = new MqttProperties();
    }

    public void recycle() {
        this.properties.recycle();
    }

    public MqttMessageIdVariableHeader(int messageId) {
        this.messageId = messageId;
        this.properties = new MqttProperties();
    }

    public int messageId() {return messageId;}

    public static MqttMessageIdVariableHeader from(int messageId) {
        if (messageId < 1 || messageId > 0xffff) {
            String errMsg = "messageId: " + messageId + " (expected: 1 ~ 65535)";
            throw new IllegalArgumentException(errMsg);
        }
        return new MqttMessageIdVariableHeader(messageId);
    }

}
