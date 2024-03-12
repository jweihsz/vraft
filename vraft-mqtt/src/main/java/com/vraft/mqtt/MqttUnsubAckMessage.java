package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:13
 **/
@Data
public class MqttUnsubAckMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private MqttUnsubAckPayload payload;

    private transient Handle<MqttUnsubAckMessage> handle;

    public MqttUnsubAckMessage(Handle<MqttUnsubAckMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.payload = new MqttUnsubAckPayload();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.payload.recycle();
        this.handle.recycle(this);
    }
}
