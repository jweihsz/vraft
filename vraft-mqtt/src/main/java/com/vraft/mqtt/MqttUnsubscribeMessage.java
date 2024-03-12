package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:09
 **/
@Data
public class MqttUnsubscribeMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttMessageIdVariableHeader variableHeader;
    private MqttUnsubscribePayload payload;
    private DecoderResult decoderResult;
}
