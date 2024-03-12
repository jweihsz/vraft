package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:28
 **/
@Data
public class MqttPublishVariableHeader {
    private String topicName;
    private int packetId;
    private MqttProperties properties;

    public MqttPublishVariableHeader() {
        this.topicName = null;
        this.packetId = 0;
        this.properties = new MqttProperties();
    }

    public void recycle() {
        this.topicName = null;
        this.packetId = 0;
        this.properties.recycle();
    }

    public MqttPublishVariableHeader(String topicName, int packetId) {
        this(topicName, packetId, MqttProperties.NO_PROPERTIES);
    }

    public MqttPublishVariableHeader(String topicName, int packetId, MqttProperties properties) {
        this.topicName = topicName;
        this.packetId = packetId;
        this.properties = MqttProperties.withEmptyDefaults(properties);
    }

}
