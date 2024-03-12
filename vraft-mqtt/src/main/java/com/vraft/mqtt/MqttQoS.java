package com.vraft.mqtt;

/**
 * @author jweihsz
 * @version 2024/3/11 19:52
 **/
public enum MqttQoS {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2),
    FAILURE(0x80);
    private final int value;

    MqttQoS(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MqttQoS valueOf(int value) {
        switch (value) {
            case 0:
                return AT_MOST_ONCE;
            case 1:
                return AT_LEAST_ONCE;
            case 2:
                return EXACTLY_ONCE;
            case 0x80:
                return FAILURE;
            default:
                throw new IllegalArgumentException("invalid QoS: " + value);
        }
    }
}
