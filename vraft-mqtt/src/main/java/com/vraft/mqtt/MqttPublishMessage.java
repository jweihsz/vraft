package com.vraft.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderResult;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:28
 **/
@Data
public class MqttPublishMessage {
    private MqttFixedHeader mqttFixedHeader;
    private MqttPublishVariableHeader variableHeader;
    private ByteBuf payload;
    private DecoderResult decoderResult;

    public MqttPublishMessage(
        MqttFixedHeader mqttFixedHeader,
        MqttPublishVariableHeader variableHeader,
        ByteBuf payload) {
        this.mqttFixedHeader = mqttFixedHeader;
        this.variableHeader = variableHeader;
        this.payload = payload;
    }

    public ByteBuf payload() {
        return content();
    }

    public ByteBuf content() {
        return ByteBufUtil.ensureAccessible(payload);
    }

    public boolean release() {
        return content().release();
    }

    public boolean release(int decrement) {
        return content().release(decrement);
    }

    public MqttPublishMessage touch() {
        content().touch();
        return this;
    }

    public MqttPublishMessage touch(Object hint) {
        content().touch(hint);
        return this;
    }

    public MqttPublishMessage retain() {
        content().retain();
        return this;
    }

    public MqttPublishMessage retain(int increment) {
        content().retain(increment);
        return this;
    }

    public int refCnt() {
        return content().refCnt();
    }

    public MqttPublishMessage copy() {
        return replace(content().copy());
    }

    public MqttPublishMessage duplicate() {
        return replace(content().duplicate());
    }

    public MqttPublishMessage retainedDuplicate() {
        return replace(content().retainedDuplicate());
    }

    public MqttPublishMessage replace(ByteBuf content) {
        return new MqttPublishMessage(mqttFixedHeader, variableHeader, content);
    }

}
