package com.vraft.mqtt;

import io.netty.util.Recycler;

/**
 * @author jweihsz
 * @version 2024/3/12 09:39
 **/
public class MqttMessagePool {

    public static final Recycler<MqttAuthMessage> AUTH_MSG_RECYCLER = new Recycler<MqttAuthMessage>() {
        @Override
        protected MqttAuthMessage newObject(Handle<MqttAuthMessage> handle) {
            return new MqttAuthMessage(handle);
        }
    };

    public static final Recycler<MqttConnectMessage> CONNECT_MSG_RECYCLER = new Recycler<MqttConnectMessage>() {
        @Override
        protected MqttConnectMessage newObject(Handle<MqttConnectMessage> handle) {
            return new MqttConnectMessage(handle);
        }
    };

    public static final Recycler<MqttConnAckMessage> CONNECT_ACK_MSG_RECYCLER = new Recycler<MqttConnAckMessage>() {
        @Override
        protected MqttConnAckMessage newObject(Handle<MqttConnAckMessage> handle) {
            return new MqttConnAckMessage(handle);
        }
    };

    public static final Recycler<MqttDisconnectMessage> DISCONNECT_MSG_RECYCLER = new Recycler<MqttDisconnectMessage>() {
        @Override
        protected MqttDisconnectMessage newObject(Handle<MqttDisconnectMessage> handle) {
            return new MqttDisconnectMessage(handle);
        }
    };

    public static final Recycler<MqttPublishMessage> PUBLISH_MSG_RECYCLER = new Recycler<MqttPublishMessage>() {
        @Override
        protected MqttPublishMessage newObject(Handle<MqttPublishMessage> handle) {
            return new MqttPublishMessage(handle);
        }
    };

    public static final Recycler<MqttPubAckMessage> PUBLISH_ACK_MSG_RECYCLER = new Recycler<MqttPubAckMessage>() {
        @Override
        protected MqttPubAckMessage newObject(Handle<MqttPubAckMessage> handle) {
            return new MqttPubAckMessage(handle);
        }
    };

    public static final Recycler<MqttSubscribeMessage> SUBSCRIBE_MSG_RECYCLER = new Recycler<MqttSubscribeMessage>() {
        @Override
        protected MqttSubscribeMessage newObject(Handle<MqttSubscribeMessage> handle) {
            return new MqttSubscribeMessage(handle);
        }
    };

    public static final Recycler<MqttSubAckMessage> SUBSCRIBE_ACK_MSG_RECYCLER = new Recycler<MqttSubAckMessage>() {
        @Override
        protected MqttSubAckMessage newObject(Handle<MqttSubAckMessage> handle) {
            return new MqttSubAckMessage(handle);
        }
    };

    public static final Recycler<MqttUnsubscribeMessage> UNSUBSCRIBE_MSG_RECYCLER = new Recycler<MqttUnsubscribeMessage>() {
        @Override
        protected MqttUnsubscribeMessage newObject(Handle<MqttUnsubscribeMessage> handle) {
            return new MqttUnsubscribeMessage(handle);
        }
    };

    public static final Recycler<MqttUnsubAckMessage> UNSUBSCRIBE_ACK_MSG_RECYCLER = new Recycler<MqttUnsubAckMessage>() {
        @Override
        protected MqttUnsubAckMessage newObject(Handle<MqttUnsubAckMessage> handle) {
            return new MqttUnsubAckMessage(handle);
        }
    };

    public static final Recycler<MqttPingReqMessage> PING_REQ_MSG_RECYCLER = new Recycler<MqttPingReqMessage>() {
        @Override
        protected MqttPingReqMessage newObject(Handle<MqttPingReqMessage> handle) {
            return new MqttPingReqMessage(handle);
        }
    };

    public static final Recycler<MqttPingRespMessage> PING_RESP_MSG_RECYCLER = new Recycler<MqttPingRespMessage>() {
        @Override
        protected MqttPingRespMessage newObject(Handle<MqttPingRespMessage> handle) {
            return new MqttPingRespMessage(handle);
        }
    };

    public static final Recycler<MqttPubRecMessage> PUB_REC_MSG_RECYCLER = new Recycler<MqttPubRecMessage>() {
        @Override
        protected MqttPubRecMessage newObject(Handle<MqttPubRecMessage> handle) {
            return new MqttPubRecMessage(handle);
        }
    };

    public static final Recycler<MqttPubRelMessage> PUB_REL_MSG_RECYCLER = new Recycler<MqttPubRelMessage>() {
        @Override
        protected MqttPubRelMessage newObject(Handle<MqttPubRelMessage> handle) {
            return new MqttPubRelMessage(handle);
        }
    };

    public static final Recycler<MqttPubCompMessage> PUB_COMP_MSG_RECYCLER = new Recycler<MqttPubCompMessage>() {
        @Override
        protected MqttPubCompMessage newObject(Handle<MqttPubCompMessage> handle) {
            return new MqttPubCompMessage(handle);
        }
    };

    public static MqttBaseMessage browObject(MqttMessageType type) {
        if (type == MqttMessageType.AUTH) {
            return AUTH_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.CONNECT) {
            return CONNECT_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.CONNACK) {
            return CONNECT_ACK_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.DISCONNECT) {
            return DISCONNECT_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PUBLISH) {
            return PUBLISH_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PUBACK) {
            return PUBLISH_ACK_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.SUBSCRIBE) {
            return SUBSCRIBE_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.SUBACK) {
            return SUBSCRIBE_ACK_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.UNSUBSCRIBE) {
            return UNSUBSCRIBE_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.UNSUBACK) {
            return UNSUBSCRIBE_ACK_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PINGREQ) {
            return PING_REQ_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PINGRESP) {
            return PING_RESP_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PUBCOMP) {
            return PUB_COMP_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PUBREC) {
            return PUB_REC_MSG_RECYCLER.get();
        }
        if (type == MqttMessageType.PUBREL) {
            return PUB_REL_MSG_RECYCLER.get();
        }
        return null;
    }

    public static void returnObject(MqttBaseMessage msg) {
        if (msg == null) {return;}
        MqttMessageType type;
        type = msg.mqttFixedHeader.getMessageType();
        if (type == MqttMessageType.AUTH) {
            ((MqttAuthMessage)msg).recycle();
        } else if (type == MqttMessageType.CONNECT) {
            ((MqttConnectMessage)msg).recycle();
        } else if (type == MqttMessageType.CONNACK) {
            ((MqttConnAckMessage)msg).recycle();
        } else if (type == MqttMessageType.DISCONNECT) {
            ((MqttDisconnectMessage)msg).recycle();
        } else if (type == MqttMessageType.PUBLISH) {
            ((MqttPublishMessage)msg).recycle();
        } else if (type == MqttMessageType.PUBACK) {
            ((MqttPubAckMessage)msg).recycle();
        } else if (type == MqttMessageType.SUBSCRIBE) {
            ((MqttSubscribeMessage)msg).recycle();
        } else if (type == MqttMessageType.SUBACK) {
            ((MqttSubAckMessage)msg).recycle();
        } else if (type == MqttMessageType.UNSUBSCRIBE) {
            ((MqttUnsubscribeMessage)msg).recycle();
        } else if (type == MqttMessageType.UNSUBACK) {
            ((MqttUnsubAckMessage)msg).recycle();
        } else if (type == MqttMessageType.PINGREQ) {
            ((MqttPingReqMessage)msg).recycle();
        } else if (type == MqttMessageType.PINGRESP) {
            ((MqttPingRespMessage)msg).recycle();
        } else if (type == MqttMessageType.PUBCOMP) {
            ((MqttPubCompMessage)msg).recycle();
        } else if (type == MqttMessageType.PUBREC) {
            ((MqttPubRecMessage)msg).recycle();
        } else if (type == MqttMessageType.PUBREL) {
            ((MqttPubRelMessage)msg).recycle();
        }
    }

    public static MqttMessageIdVariableHeader getFromBase(MqttBaseMessage msg) {
        MqttMessageType type;
        type = msg.mqttFixedHeader.getMessageType();
        if (type == MqttMessageType.UNSUBSCRIBE) {
            return ((MqttUnsubscribeMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.SUBSCRIBE) {
            return ((MqttSubscribeMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.UNSUBACK) {
            return ((MqttUnsubAckMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.SUBACK) {
            return ((MqttSubAckMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.PUBACK) {
            return ((MqttPubAckMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.PUBCOMP) {
            return ((MqttPubCompMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.PUBREC) {
            return ((MqttPubRecMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.PUBREL) {
            return ((MqttPubRelMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.DISCONNECT) {
            return ((MqttDisconnectMessage)msg).getVariableHeader();
        } else if (type == MqttMessageType.AUTH) {
            return ((MqttAuthMessage)msg).getVariableHeader();
        }
        return null;
    }
}
