package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 21:38
 **/
@Data
public class MqttPingRespMessage extends MqttBaseMessage {
    private transient Handle<MqttPingRespMessage> handle;

    public MqttPingRespMessage(Handle<MqttPingRespMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.handle.recycle(this);
    }
}