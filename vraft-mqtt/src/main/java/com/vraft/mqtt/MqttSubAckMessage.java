package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:05
 **/
@Data
public class MqttSubAckMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttMessageIdVariableHeader variableHeader;
    private MqttSubAckPayload payload;
}
