package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:20
 **/
@Data
public class MqttConnectPayload {
    private String willTopic;
    private byte[] willMessage;
    private String userName;
    private byte[] password;
    private String clientIdentifier;
    private MqttProperties willProperties;
    private int numberOfBytesConsumed;

    public MqttConnectPayload() {
        this.willProperties = new MqttProperties();
    }

    public void recycle() {
        this.willTopic = null;
        this.willMessage = null;
        this.userName = null;
        this.password = null;
        this.clientIdentifier = null;
        this.numberOfBytesConsumed = 0;
        this.willProperties.recycle();
    }
}
