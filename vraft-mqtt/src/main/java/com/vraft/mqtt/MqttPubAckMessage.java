package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:47
 **/
@Data
public class MqttPubAckMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private transient Handle<MqttPubAckMessage> handle;

    public MqttPubAckMessage(Handle<MqttPubAckMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttMessageIdVariableHeader();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.handle.recycle(this);
    }
}
