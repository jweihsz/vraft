package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:19
 **/
@Data
public class MqttConnectVariableHeader {
    private String name;
    private int version;
    private boolean hasUserName;
    private boolean hasPassword;
    private boolean isWillRetain;
    private int willQos;
    private boolean isWillFlag;
    private boolean isCleanSession;
    private int keepAliveTimeSeconds;
    private MqttProperties properties;

    public MqttConnectVariableHeader() {
        this.properties = new MqttProperties();
    }

    public void recycle() {
        this.properties.recycle();
        this.name = null;
    }

}
