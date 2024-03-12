package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jweihsz
 * @version 2024/3/11 21:39
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class MqttAuthMessage extends MqttBaseMessage {
    private MqttMessageIdVariableHeader variableHeader;
    private transient Handle<MqttAuthMessage> handle;

    public MqttAuthMessage(Handle<MqttAuthMessage> handle) {
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
