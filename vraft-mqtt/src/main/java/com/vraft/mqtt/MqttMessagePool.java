package com.vraft.mqtt;

import io.netty.util.Recycler;

/**
 * @author jweihsz
 * @version 2024/3/12 09:39
 **/
public class MqttMessagePool {

    public static final Recycler<MqttConnectMessage> CONNECT_MESSAGE_RECYCLER = new Recycler<MqttConnectMessage>() {
        @Override
        protected MqttConnectMessage newObject(Handle<MqttConnectMessage> handle) {
            return new MqttConnectMessage(handle);
        }
    };

    public static Object browObject(MqttMessageType type) {
        switch (type) {
            case AUTH: { }
            case CONNECT: {return CONNECT_MESSAGE_RECYCLER.get();}
            case CONNACK: {

            }
            case DISCONNECT: {

            }
            case PUBLISH: {

            }
            case PUBACK: {

            }
            case SUBSCRIBE: {

            }
            case SUBACK: {

            }
            case PINGREQ: {

            }
            case PINGRESP: {

            }
            case PUBCOMP: {

            }
            case PUBREC: {

            }
            case PUBREL: {

            }
            default: {return null;}
        }
    }
}
