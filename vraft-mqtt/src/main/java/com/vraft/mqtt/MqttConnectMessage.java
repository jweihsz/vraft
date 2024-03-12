package com.vraft.mqtt;

import io.netty.util.Recycler.Handle;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:14
 **/
@Data
public class MqttConnectMessage extends MqttBaseMessage {
    private MqttConnectVariableHeader variableHeader;
    private MqttConnectPayload payload;
    private DecoderResult decoderResult;
    private transient Handle<MqttConnectMessage> handle;

    public MqttConnectMessage(Handle<MqttConnectMessage> handle) {
        this.handle = handle;
        this.mqttFixedHeader = new MqttFixedHeader();
        this.variableHeader = new MqttConnectVariableHeader();
        this.payload = new MqttConnectPayload();
        this.decoderResult = new DecoderResult();
    }

    public void recycle() {
        this.mqttFixedHeader.recycle();
        this.variableHeader.recycle();
        this.payload.recycle();
        this.decoderResult.recycle();
        this.handle.recycle(this);
    }

}
