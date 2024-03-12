package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:05
 **/
@Data
public class MqttSubAckMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private MqttSubAckPayload payload;
    private transient Handle<MqttSubAckMessage> handle;

    public MqttSubAckMessage(Handle<MqttSubAckMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.payload = new MqttSubAckPayload();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.payload.recycle();
        this.handle.recycle(this);
    }
}
