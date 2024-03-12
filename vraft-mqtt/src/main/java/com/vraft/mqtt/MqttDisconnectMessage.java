package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:39
 **/
@Data
public class MqttDisconnectMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private transient Handle<MqttDisconnectMessage> handle;

    public MqttDisconnectMessage(Handle<MqttDisconnectMessage> handle) {
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
