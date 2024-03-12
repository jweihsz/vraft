package com.vraft.mqtt;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author jweihsz
 * @version 2024/3/11 21:19
 **/
public class MqttCodecUtil {
    private MqttCodecUtil() { }

    public static final char[] TOPIC_WILDCARDS = {'#', '+'};

    static final AttributeKey<MqttVersion> MQTT_VERSION_KEY = AttributeKey.valueOf("NETTY_CODEC_MQTT_VERSION");

    static MqttVersion getMqttVersion(ChannelHandlerContext ctx) {
        Attribute<MqttVersion> attr = ctx.channel().attr(MQTT_VERSION_KEY);
        MqttVersion version = attr.get();
        return version == null ? MqttVersion.MQTT_3_1_1 : version;
    }

    static void setMqttVersion(ChannelHandlerContext ctx, MqttVersion version) {
        Attribute<MqttVersion> attr = ctx.channel().attr(MQTT_VERSION_KEY);
        attr.set(version);
    }

    static boolean isValidPublishTopicName(String topicName) {
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {return false;}
        }
        return true;
    }

    static boolean isValidMessageId(int messageId) {
        return messageId != 0;
    }

    static boolean isValidClientId(MqttVersion mqttVersion, int maxClientIdLength, String clientId) {
        if (mqttVersion == MqttVersion.MQTT_3_1) {
            return clientId != null && clientId.length() >= MqttConstant.MIN_CLIENT_ID_LENGTH
                && clientId.length() <= maxClientIdLength;
        }
        if (mqttVersion == MqttVersion.MQTT_3_1_1 || mqttVersion == MqttVersion.MQTT_5) {
            return clientId != null;
        }
        String errMsg = mqttVersion + " is unknown mqtt version";
        throw new IllegalArgumentException(errMsg);
    }

    public static MqttFixedHeader validateFixedHeader(ChannelHandlerContext ctx, MqttFixedHeader mqttFixedHeader) {
        String errMsg = null;
        switch (mqttFixedHeader.getMessageType()) {
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (mqttFixedHeader.getQosLevel() != MqttQoS.AT_LEAST_ONCE) {
                    errMsg = mqttFixedHeader.getMessageType().name() + " message must have QoS 1";
                    throw new RuntimeException(errMsg);
                }
                return mqttFixedHeader;
            case AUTH:
                if (MqttCodecUtil.getMqttVersion(ctx) != MqttVersion.MQTT_5) {
                    throw new RuntimeException("AUTH message requires at least MQTT 5");
                }
                return mqttFixedHeader;
            default:
                return mqttFixedHeader;
        }
    }

    public static MqttFixedHeader resetUnusedFields(MqttFixedHeader mqttFixedHeader) {
        switch (mqttFixedHeader.getMessageType()) {
            case CONNECT:
            case CONNACK:
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case SUBACK:
            case UNSUBACK:
            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                if (mqttFixedHeader.isDup() ||
                    mqttFixedHeader.getQosLevel() != MqttQoS.AT_MOST_ONCE ||
                    mqttFixedHeader.isRetain()) {
                    mqttFixedHeader.setDup(false);
                    mqttFixedHeader.setQosLevel(MqttQoS.AT_MOST_ONCE);
                    mqttFixedHeader.setRetain(false);
                }
                return mqttFixedHeader;
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (mqttFixedHeader.isRetain()) {
                    mqttFixedHeader.setRetain(false);
                }
                return mqttFixedHeader;
            default:
                return mqttFixedHeader;
        }
    }

}
