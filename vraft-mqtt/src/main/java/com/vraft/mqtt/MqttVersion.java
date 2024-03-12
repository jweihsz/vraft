package com.vraft.mqtt;

import java.nio.charset.StandardCharsets;

/**
 * @author jweihsz
 * @version 2024/3/11 19:48
 **/
public enum MqttVersion {
    MQTT_3_1("MQIsdp", (byte)3),
    MQTT_3_1_1("MQTT", (byte)4),
    MQTT_5("MQTT", (byte)5);

    private final String name;
    private final byte level;

    MqttVersion(String protocolName, byte protocolLevel) {
        name = protocolName;
        level = protocolLevel;
    }

    public String protocolName() {return name;}

    public byte[] protocolNameBytes() {
        return name.getBytes(StandardCharsets.UTF_8);
    }

    public byte protocolLevel() {return level;}

    public static MqttVersion fromProtocolNameAndLevel(
        String protocolName, byte protocolLevel) {
        String errMsg = null;
        MqttVersion mv = null;
        switch (protocolLevel) {
            case 3:
                mv = MQTT_3_1;
                break;
            case 4:
                mv = MQTT_3_1_1;
                break;
            case 5:
                mv = MQTT_5;
                break;
            default:
                break;
        }
        if (mv == null) {
            errMsg = protocolName + " is an unknown protocol name";
            throw new RuntimeException(errMsg);
        }
        if (mv.name.equals(protocolName)) {return mv;}
        errMsg = protocolName + " and " + protocolLevel + " don't match";
        throw new RuntimeException(errMsg);
    }
}
