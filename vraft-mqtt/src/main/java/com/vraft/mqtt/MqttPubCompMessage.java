package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:41
 **/
@Data
public class MqttPubCompMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private transient Handle<MqttPubCompMessage> handle;

    public MqttPubCompMessage(Handle<MqttPubCompMessage> handle) {
        this.handle = handle;
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.mqttFixedHeader = new MqttFixedHeader();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.handle.recycle(this);
    }
}
