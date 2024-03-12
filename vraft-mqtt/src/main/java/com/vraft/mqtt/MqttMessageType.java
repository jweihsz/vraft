package com.vraft.mqtt;

/**
 * @author jweihsz
 * @version 2024/3/11 19:53
 **/
public enum MqttMessageType {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    AUTH(15);
    private static final MqttMessageType[] VALUES;

    static {
        final MqttMessageType[] values = values();
        VALUES = new MqttMessageType[values.length + 1];
        for (MqttMessageType mqttMessageType : values) {
            final int value = mqttMessageType.value;
            if (VALUES[value] != null) {
                throw new AssertionError("value already in use: " + value);
            }
            VALUES[value] = mqttMessageType;
        }
    }

    private final int value;

    MqttMessageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MqttMessageType valueOf(int type) {
        if (type <= 0 || type >= VALUES.length) {
            throw new IllegalArgumentException("unknown message type: " + type);
        }
        return VALUES[type];
    }
}