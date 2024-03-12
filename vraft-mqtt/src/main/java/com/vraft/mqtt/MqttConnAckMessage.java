package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:24
 **/
@Data
public class MqttConnAckMessage extends MqttBaseMessage {
    private MqttConnAckVariableHeader variableHeader;
    private transient Handle<MqttConnAckMessage> handle;

    public MqttConnAckMessage(Handle<MqttConnAckMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttConnAckVariableHeader();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.handle.recycle(this);
    }
}
