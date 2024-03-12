package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:23
 **/
@Data
public class MqttConnAckVariableHeader {
    private final MqttConnectReturnCode connectReturnCode;

    private final boolean sessionPresent;

    private final MqttProperties properties;

    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode,
        boolean sessionPresent) {
        this(connectReturnCode, sessionPresent, MqttProperties.NO_PROPERTIES);
    }

    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode,
        boolean sessionPresent, MqttProperties properties) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
        this.properties = MqttProperties.withEmptyDefaults(properties);
    }
}
