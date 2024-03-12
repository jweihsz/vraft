package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:23
 **/
@Data
public class MqttConnAckVariableHeader {
    private MqttConnectReturnCode connectReturnCode;
    
    private boolean sessionPresent;

    private MqttProperties properties;

    public MqttConnAckVariableHeader() {
        this.properties = new MqttProperties();
    }

    public void recycle() {
        this.properties.recycle();
    }

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
