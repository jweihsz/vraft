package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:40
 **/
@Data
public class MqttPubRecMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private transient Handle<MqttPubRecMessage> handle;

    public MqttPubRecMessage(Handle<MqttPubRecMessage> handle) {
        this.handle = handle;
        this.variableHeader = new MqttMessageIdVariableHeader();
        this.mqttFixedHeader = new MqttFixedHeader();
    }

    public void recycle() {
        this.variableHeader.recycle();
        this.mqttFixedHeader.recycle();
        this.handle.recycle(this);
    }
}
