package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:57
 **/
@Data
public class MqttSubscribeMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private MqttSubscribePayload payload;
    private DecoderResult decoderResult;
    private transient Handle<MqttSubscribeMessage> handle;

    public MqttSubscribeMessage(Handle<MqttSubscribeMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.payload = new MqttSubscribePayload();
        this.decoderResult = null;
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.payload.recycle();
        this.decoderResult = null;
        this.handle.recycle(this);
    }
}
