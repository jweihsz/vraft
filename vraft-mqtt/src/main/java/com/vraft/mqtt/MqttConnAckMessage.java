package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:24
 **/
@Data
public class MqttConnAckMessage {
    private final MqttFixedHeader mqttFixedHeader;
    private final MqttConnAckVariableHeader variableHeader;
}
