package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:47
 **/
@Data
public class MqttPubAckMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttMessageIdVariableHeader variableHeader;
}
