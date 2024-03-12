package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:57
 **/
@Data
public class MqttSubscribeMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttMessageIdVariableHeader variableHeader;
    private MqttSubscribePayload payload;
    private DecoderResult decoderResult;
}
