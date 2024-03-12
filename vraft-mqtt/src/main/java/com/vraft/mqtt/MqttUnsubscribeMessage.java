package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:09
 **/
@Data
public class MqttUnsubscribeMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private MqttUnsubscribePayload payload;
    private DecoderResult decoderResult;

    private transient Handle<MqttUnsubscribeMessage> handle;

    public MqttUnsubscribeMessage(Handle<MqttUnsubscribeMessage> handle) {
        this.handle = handle;
        this.decoderResult = null;
        this.payload = new MqttUnsubscribePayload();
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.mqttFixedHeader = new MqttFixedHeader();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.payload.recycle();
        this.decoderResult = null;
        this.handle.recycle(this);
    }
}
