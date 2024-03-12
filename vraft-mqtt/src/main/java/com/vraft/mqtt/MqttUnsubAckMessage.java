package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:13
 **/
@Data
public class MqttUnsubAckMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttMessageIdVariableHeader variableHeader;
    private MqttUnsubAckPayload payload;
}
