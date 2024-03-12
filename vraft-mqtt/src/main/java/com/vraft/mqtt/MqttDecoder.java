package com.vraft.mqtt;

import java.util.List;

import com.vraft.mqtt.MqttDecoder.DecoderState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;

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

    private static MqttFixedHeader decodeFixedHeader(ChannelHandlerContext ctx, ByteBuf buffer) {
        String errMsg = null;
        Object mqttMsg = null;
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
        MqttFixedHeader decodedFixedHeader =
            new MqttFixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
        return validateFixedHeader(ctx, resetUnusedFields(decodedFixedHeader));
    }

}
