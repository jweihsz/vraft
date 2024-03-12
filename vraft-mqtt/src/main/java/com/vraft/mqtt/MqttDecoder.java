package com.vraft.mqtt;

import java.util.List;

import com.vraft.mqtt.MqttDecoder.DecoderState;
import com.vraft.mqtt.MqttProperties.IntegerProperty;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:24
 **/
public class MqttDecoder extends ReplayingDecoder<DecoderState> {

    enum DecoderState {
        READ_FIXED_HEADER,
        READ_VARIABLE_HEADER,
        READ_PAYLOAD,
        BAD_MESSAGE,
    }

    private Object mqttMessage;
    private MqttMessageType messageType;
    private int bytesRemainingInVariablePart;
    private final int maxBytesInMessage;
    private final int maxClientIdLength;

    public MqttDecoder() {
        this(MqttConstant.DEFAULT_MAX_BYTES_IN_MESSAGE, MqttConstant.DEFAULT_MAX_CLIENT_ID_LENGTH);
    }

    public MqttDecoder(int maxBytesInMessage) {
        this(maxBytesInMessage, MqttConstant.DEFAULT_MAX_CLIENT_ID_LENGTH);
    }

    public MqttDecoder(int maxBytesInMessage, int maxClientIdLength) {
        super(DecoderState.READ_FIXED_HEADER);
        this.maxBytesInMessage = maxBytesInMessage;
        this.maxClientIdLength = maxClientIdLength;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {

    }

    private static MqttBaseMessage decodeFixedHeader(ChannelHandlerContext ctx, ByteBuf buffer) {
        String errMsg = null;
        MqttMessageType messageType;
        short b1 = buffer.readUnsignedByte();
        messageType = MqttMessageType.valueOf(b1 >> 4);
        boolean dupFlag = (b1 & 0x08) == 0x08;
        int qosLevel = (b1 & 0x06) >> 1;
        boolean retain = (b1 & 0x01) != 0;
        switch (messageType) {
            case PUBLISH: {
                if (qosLevel == 3) {
                    errMsg = "Illegal QOS Level in fixed "
                        + "header of PUBLISH message (" + qosLevel + ')';
                    throw new DecoderException(errMsg);
                }
                break;
            }
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (dupFlag) {
                    errMsg = "Illegal BIT 3 in fixed header of " + messageType
                        + " message, must be 0, found 1";
                    throw new DecoderException(errMsg);
                }
                if (qosLevel != 1) {
                    errMsg = "Illegal QOS Level in fixed header of " + messageType
                        + " message, must be 1, found " + qosLevel;
                    throw new DecoderException(errMsg);
                }
                if (retain) {
                    errMsg = "Illegal BIT 0 in fixed header of " + messageType
                        + " message, must be 0, found 1";
                    throw new DecoderException(errMsg);
                }
                break;
            case AUTH:
            case CONNACK:
            case CONNECT:
            case DISCONNECT:
            case PINGREQ:
            case PINGRESP:
            case PUBACK:
            case PUBCOMP:
            case PUBREC:
            case SUBACK:
            case UNSUBACK:
                if (dupFlag) {
                    errMsg = "Illegal BIT 3 in fixed header of " + messageType
                        + " message, must be 0, found 1";
                    throw new DecoderException(errMsg);
                }
                if (qosLevel != 0) {
                    errMsg = "Illegal BIT 2 or 1 in fixed header of " + messageType
                        + " message, must be 0, found " + qosLevel;
                    throw new DecoderException(errMsg);
                }
                if (retain) {
                    errMsg = "Illegal BIT 0 in fixed header of " + messageType
                        + " message, must be 0, found 1";
                    throw new DecoderException(errMsg);
                }
                break;
            default:
                errMsg = "Unknown message type, do not know how to validate fixed header";
                throw new DecoderException(errMsg);
        }
        int remainingLength = 0;
        int multiplier = 1;
        short digit;
        int loops = 0;
        do {
            digit = buffer.readUnsignedByte();
            remainingLength += (digit & 127) * multiplier;
            multiplier *= 128;
            loops++;
        } while ((digit & 128) != 0 && loops < 4);
        if (loops == 4 && (digit & 128) != 0) {
            throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
        }
        MqttBaseMessage base = MqttMessagePool.browObject(messageType);
        if (base == null) {
            throw new DecoderException("unknow message type");
        }
        MqttFixedHeader header = base.mqttFixedHeader;
        header.setMessageType(messageType);
        header.setDup(dupFlag);
        header.setQosLevel(MqttQoS.valueOf(qosLevel));
        header.setRetain(retain);
        header.setRemainingLength(remainingLength);
        try {
            MqttCodecUtil.resetUnusedFields(header);
            MqttCodecUtil.validateFixedHeader(ctx, header);
            return base;
        } catch (Exception ex) {
            MqttMessagePool.returnObject(base);
            throw new RuntimeException(ex);
        }
    }

    private int decodeVariableHeader(ChannelHandlerContext ctx, ByteBuf buffer, MqttBaseMessage base) {
        final MqttFixedHeader mqttFixedHeader = base.mqttFixedHeader;
        switch (mqttFixedHeader.messageType()) {
            case CONNECT:
                return decodeConnectionVariableHeader(ctx, base, buffer);
            case CONNACK:
                return decodeConnAckVariableHeader(ctx, base, buffer);
            case UNSUBSCRIBE:
            case SUBSCRIBE:
            case SUBACK:
            case UNSUBACK:
                return decodeMessageIdAndPropertiesVariableHeader(ctx, base, buffer);
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case PUBREL:
                return decodePubReplyMessage(buffer, base);
            case PUBLISH:
                return decodePublishVariableHeader(ctx, buffer, base);
            case DISCONNECT:
            case AUTH:
                return decodeReasonCodeAndPropertiesVariableHeader(buffer, base);
            case PINGREQ:
            case PINGRESP:
                return 0;
            default:
                throw new DecoderException("Unknown message type: " + mqttFixedHeader.messageType());
        }
    }

    private int decodeReasonCodeAndPropertiesVariableHeader(
        ByteBuf buffer, MqttBaseMessage base) {
        byte reasonCode;
        final int consumed;
        Result<String> res = new Result<>();
        MqttMessageIdVariableHeader variableHeader = MqttMessagePool.getFromBase(base);
        final MqttProperties properties = variableHeader.getProperties();
        if (bytesRemainingInVariablePart > 1) {
            reasonCode = buffer.readByte();
            decodeProperties(buffer, properties, res);
            consumed = 1 + res.numberOfBytesConsumed;
        } else if (bytesRemainingInVariablePart > 0) {
            reasonCode = buffer.readByte();
            consumed = 1;
        } else {
            reasonCode = 0;
            consumed = 0;
        }
        variableHeader.setReasonCode(reasonCode);
        return consumed;
    }

    private int decodePublishVariableHeader(
        ChannelHandlerContext ctx, ByteBuf buffer, MqttBaseMessage base) {
        Result<String> res = new Result<>();
        MqttFixedHeader mqttFixedHeader = base.mqttFixedHeader;
        final MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
        decodeString(buffer, res);
        if (!isValidPublishTopicName(res.value)) {
            throw new DecoderException("invalid publish topic name: "
                + res.value + " (contains wildcards)");
        }
        final String decodedTopic = res.value;
        int numberOfBytesConsumed = res.numberOfBytesConsumed;
        int messageId = -1;
        if (mqttFixedHeader.qosLevel().value() > 0) {
            messageId = decodeMessageId(buffer);
            numberOfBytesConsumed += 2;
        }
        MqttPublishMessage message = (MqttPublishMessage)base;
        MqttPublishVariableHeader var = message.getVariableHeader();
        MqttProperties properties = var.getProperties();
        if (mqttVersion == MqttVersion.MQTT_5) {
            decodeProperties(buffer, properties, res);
            numberOfBytesConsumed += res.numberOfBytesConsumed;
        } else {
            //properties = MqttProperties.NO_PROPERTIES;
        }
        var.setTopicName(decodedTopic);
        var.setPacketId(messageId);
        return numberOfBytesConsumed;
    }

    private int decodePubReplyMessage(ByteBuf buffer, MqttBaseMessage base) {
        Result<String> res = new Result<>();
        final int packetId = decodeMessageId(buffer);
        MqttMessageIdVariableHeader mqttPubAckVariableHeader;
        mqttPubAckVariableHeader = MqttMessagePool.getFromBase(base);
        MqttProperties properties = mqttPubAckVariableHeader.getProperties();
        final int consumed;
        final int packetIdNumberOfBytesConsumed = 2;
        if (bytesRemainingInVariablePart > 3) {
            final byte reasonCode = buffer.readByte();
            final int num = decodeProperties(buffer, properties, res);
            mqttPubAckVariableHeader.setMessageId(packetId);
            mqttPubAckVariableHeader.setReasonCode(reasonCode);
            consumed = packetIdNumberOfBytesConsumed + 1 + num;
        } else if (bytesRemainingInVariablePart > 2) {
            final byte reasonCode = buffer.readByte();
            mqttPubAckVariableHeader.setMessageId(packetId);
            mqttPubAckVariableHeader.setReasonCode(reasonCode);
            consumed = packetIdNumberOfBytesConsumed + 1;
        } else {
            mqttPubAckVariableHeader.setMessageId(packetId);
            mqttPubAckVariableHeader.setReasonCode((byte)0);
            consumed = packetIdNumberOfBytesConsumed;
        }

        return consumed;
    }

    private static int decodeMessageIdAndPropertiesVariableHeader(
        ChannelHandlerContext ctx, MqttBaseMessage base, ByteBuf buffer) {
        Result<String> res = new Result<>();
        final MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
        final int packetId = decodeMessageId(buffer);
        MqttMessageIdVariableHeader mqttVariableHeader = MqttMessagePool.getFromBase(base);
        MqttProperties properties = mqttVariableHeader.getProperties();
        final int mqtt5Consumed;
        if (mqttVersion == MqttVersion.MQTT_5) {
            mqttVariableHeader.setMessageId(packetId);
            mqtt5Consumed = decodeProperties(buffer, properties, res);
        } else {
            mqttVariableHeader.setMessageId(packetId);
            mqtt5Consumed = 0;
        }
        return 2 + mqtt5Consumed;
    }

    private static int decodeConnAckVariableHeader(
        ChannelHandlerContext ctx, MqttBaseMessage base, ByteBuf buffer) {
        final MqttVersion mqttVersion = MqttCodecUtil.getMqttVersion(ctx);
        final boolean sessionPresent = (buffer.readUnsignedByte() & 0x01) == 0x01;
        byte returnCode = buffer.readByte();
        int numberOfBytesConsumed = 2;
        Result<String> res = new Result<>();
        MqttConnAckMessage message = (MqttConnAckMessage)base;
        MqttConnAckVariableHeader variableHeader = message.getVariableHeader();
        final MqttProperties properties = variableHeader.getProperties();
        if (mqttVersion == MqttVersion.MQTT_5) {
            numberOfBytesConsumed += decodeProperties(buffer, properties, res);
        } else {
            // properties = MqttProperties.NO_PROPERTIES;
        }
        variableHeader.setConnectReturnCode(MqttConnectReturnCode.valueOf(returnCode));
        variableHeader.setSessionPresent(sessionPresent);
        return numberOfBytesConsumed;
    }

    private static int decodeConnectionVariableHeader(
        ChannelHandlerContext ctx, MqttBaseMessage base, ByteBuf buffer) {
        Result<String> res = new Result<>();
        decodeString(buffer, res);
        int numberOfBytesConsumed = res.numberOfBytesConsumed;

        final byte protocolLevel = buffer.readByte();
        numberOfBytesConsumed += 1;

        MqttVersion version = MqttVersion.fromProtocolNameAndLevel(res.value, protocolLevel);
        MqttCodecUtil.setMqttVersion(ctx, version);

        final int b1 = buffer.readUnsignedByte();
        numberOfBytesConsumed += 1;

        final int keepAlive = decodeMsbLsb(buffer);
        numberOfBytesConsumed += 2;

        final boolean hasUserName = (b1 & 0x80) == 0x80;
        final boolean hasPassword = (b1 & 0x40) == 0x40;
        final boolean willRetain = (b1 & 0x20) == 0x20;
        final int willQos = (b1 & 0x18) >> 3;
        final boolean willFlag = (b1 & 0x04) == 0x04;
        final boolean cleanSession = (b1 & 0x02) == 0x02;
        if (version == MqttVersion.MQTT_3_1_1 || version == MqttVersion.MQTT_5) {
            final boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
            if (!zeroReservedFlag) {
                // MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
                // set to zero and disconnect the Client if it is not zero.
                // See https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
                throw new DecoderException("non-zero reserved flag");
            }
        }
        MqttConnectMessage message = (MqttConnectMessage)base;
        MqttConnectVariableHeader variableHeader = message.getVariableHeader();
        final MqttProperties properties = variableHeader.getProperties();
        if (version == MqttVersion.MQTT_5) {
            res.recycle();
            numberOfBytesConsumed += decodeProperties(buffer, properties, res);
        } else {
            //properties = MqttProperties.NO_PROPERTIES;
        }
        variableHeader.setName(version.protocolName());
        variableHeader.setVersion(version.protocolLevel());
        variableHeader.setHasUserName(hasUserName);
        variableHeader.setHasPassword(hasPassword);
        variableHeader.setWillRetain(willRetain);
        variableHeader.setWillQos(willQos);
        variableHeader.setWillFlag(willFlag);
        variableHeader.setCleanSession(cleanSession);
        variableHeader.setKeepAliveTimeSeconds(keepAlive);
        return numberOfBytesConsumed;
    }

    private static int decodeProperties(ByteBuf buffer, MqttProperties decodedProperties, Result<String> res) {
        final long propertiesLength = decodeVariableByteInteger(buffer);
        int totalPropertiesLength = unpackA(propertiesLength);
        int numberOfBytesConsumed = unpackB(propertiesLength);
        while (numberOfBytesConsumed < totalPropertiesLength) {
            long propertyId = decodeVariableByteInteger(buffer);
            final int propertyIdValue = unpackA(propertyId);
            numberOfBytesConsumed += unpackB(propertyId);
            MqttProperties.MqttPropertyType propertyType;
            propertyType = MqttProperties.MqttPropertyType.valueOf(propertyIdValue);
            switch (propertyType) {
                case PAYLOAD_FORMAT_INDICATOR:
                case REQUEST_PROBLEM_INFORMATION:
                case REQUEST_RESPONSE_INFORMATION:
                case MAXIMUM_QOS:
                case RETAIN_AVAILABLE:
                case WILDCARD_SUBSCRIPTION_AVAILABLE:
                case SUBSCRIPTION_IDENTIFIER_AVAILABLE:
                case SHARED_SUBSCRIPTION_AVAILABLE:
                    final int b1 = buffer.readUnsignedByte();
                    numberOfBytesConsumed++;
                    decodedProperties.add(new IntegerProperty(propertyIdValue, b1));
                    break;
                case SERVER_KEEP_ALIVE:
                case RECEIVE_MAXIMUM:
                case TOPIC_ALIAS_MAXIMUM:
                case TOPIC_ALIAS:
                    final int int2BytesResult = decodeMsbLsb(buffer);
                    numberOfBytesConsumed += 2;
                    decodedProperties.add(new IntegerProperty(propertyIdValue, int2BytesResult));
                    break;
                case PUBLICATION_EXPIRY_INTERVAL:
                case SESSION_EXPIRY_INTERVAL:
                case WILL_DELAY_INTERVAL:
                case MAXIMUM_PACKET_SIZE:
                    final int maxPacketSize = buffer.readInt();
                    numberOfBytesConsumed += 4;
                    decodedProperties.add(new IntegerProperty(propertyIdValue, maxPacketSize));
                    break;
                case SUBSCRIPTION_IDENTIFIER:
                    long vbIntegerResult = decodeVariableByteInteger(buffer);
                    numberOfBytesConsumed += unpackB(vbIntegerResult);
                    decodedProperties.add(new IntegerProperty(propertyIdValue, unpackA(vbIntegerResult)));
                    break;
                case CONTENT_TYPE:
                case RESPONSE_TOPIC:
                case ASSIGNED_CLIENT_IDENTIFIER:
                case AUTHENTICATION_METHOD:
                case RESPONSE_INFORMATION:
                case SERVER_REFERENCE:
                case REASON_STRING:
                    decodeString(buffer, res);
                    numberOfBytesConsumed += res.numberOfBytesConsumed;
                    decodedProperties.add(new MqttProperties.StringProperty(propertyIdValue, res.value));
                    res.recycle();
                    break;
                case USER_PROPERTY:
                    decodeString(buffer, res);
                    numberOfBytesConsumed += res.numberOfBytesConsumed;
                    final String key = res.value;
                    decodeString(buffer, res);
                    numberOfBytesConsumed += res.numberOfBytesConsumed;
                    final String value = res.value;
                    decodedProperties.add(new MqttProperties.UserProperty(key, value));
                    res.recycle();
                    break;
                case CORRELATION_DATA:
                case AUTHENTICATION_DATA:
                    final byte[] binaryDataResult = decodeByteArray(buffer);
                    numberOfBytesConsumed += binaryDataResult.length + 2;
                    decodedProperties.add(new MqttProperties.BinaryProperty(propertyIdValue, binaryDataResult));
                    break;
                default:
                    //shouldn't reach here
                    throw new DecoderException("Unknown property type: " + propertyType);
            }
        }
        return numberOfBytesConsumed;
    }

    public static boolean isValidPublishTopicName(String topicName) {
        // publish topic name must not contain any wildcard
        for (char c : MqttCodecUtil.TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static long decodeVariableByteInteger(ByteBuf buffer) {
        int remainingLength = 0;
        int multiplier = 1;
        short digit;
        int loops = 0;
        do {
            digit = buffer.readUnsignedByte();
            remainingLength += (digit & 127) * multiplier;
            multiplier *= 128;
            loops++;
        } while ((digit & 128) != 0 && loops < 4);

        if (loops == 4 && (digit & 128) != 0) {
            throw new DecoderException("MQTT protocol limits Remaining Length to 4 bytes");
        }
        return packInts(remainingLength, loops);
    }

    private static int decodeMessageId(ByteBuf buffer) {
        final int messageId = decodeMsbLsb(buffer);
        if (!isValidMessageId(messageId)) {
            throw new DecoderException("invalid messageId: " + messageId);
        }
        return messageId;
    }

    private static boolean isValidMessageId(int messageId) {
        return messageId != 0;
    }

    private static byte[] decodeByteArray(ByteBuf buffer) {
        int size = decodeMsbLsb(buffer);
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static long packInts(int a, int b) {
        return (((long)a) << 32) | (b & 0xFFFFFFFFL);
    }

    private static int unpackA(long ints) {
        return (int)(ints >> 32);
    }

    private static int unpackB(long ints) {
        return (int)ints;
    }

    private static void decodeString(ByteBuf buffer, Result<String> res) {
        decodeString(buffer, 0, Integer.MAX_VALUE, res);
    }

    private static void decodeString(ByteBuf buffer, int minBytes, int maxBytes, Result<String> res) {
        int size = decodeMsbLsb(buffer);
        int numberOfBytesConsumed = 2;
        if (size < minBytes || size > maxBytes) {
            buffer.skipBytes(size);
            numberOfBytesConsumed += size;
            res.setValue(null);
            res.setNumberOfBytesConsumed(numberOfBytesConsumed);
        }
        String s = buffer.toString(buffer.readerIndex(), size, CharsetUtil.UTF_8);
        buffer.skipBytes(size);
        numberOfBytesConsumed += size;
        res.setValue(s);
        res.setNumberOfBytesConsumed(numberOfBytesConsumed);
    }

    private static int decodeMsbLsb(ByteBuf buffer) {
        int min = 0;
        int max = 65535;
        short msbSize = buffer.readUnsignedByte();
        short lsbSize = buffer.readUnsignedByte();
        int result = msbSize << 8 | lsbSize;
        if (result < min || result > max) {
            result = -1;
        }
        return result;
    }

    @Data
    private static final class Result<T> {
        private T value;
        private int numberOfBytesConsumed;

        public Result(T value, int numberOfBytesConsumed) {
            this.value = value;
            this.numberOfBytesConsumed = numberOfBytesConsumed;
        }

        public Result() {
            this.value = null;
            this.numberOfBytesConsumed = 0;
        }

        public void recycle() {
            this.value = null;
            this.numberOfBytesConsumed = 0;
        }
    }

}
