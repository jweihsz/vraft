package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:28
 **/
@Data
public class MqttPublishVariableHeader {
    private final String topicName;
    private final int packetId;
    private final MqttProperties properties;

    public MqttPublishVariableHeader(String topicName, int packetId) {
        this(topicName, packetId, MqttProperties.NO_PROPERTIES);
    }

    public MqttPublishVariableHeader(String topicName, int packetId, MqttProperties properties) {
        this.topicName = topicName;
        this.packetId = packetId;
        this.properties = MqttProperties.withEmptyDefaults(properties);
    }

}
