package com.vraft.mqtt;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:49
 **/
@Data
public final class MqttSubscriptionOption {

    public enum RetainedHandlingPolicy {
        SEND_AT_SUBSCRIBE(0),
        SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS(1),
        DONT_SEND_AT_SUBSCRIBE(2);

        private final int value;

        RetainedHandlingPolicy(int value) {this.value = value;}

        public int value() {return value;}

        public static RetainedHandlingPolicy valueOf(int value) {
            switch (value) {
                case 0:
                    return SEND_AT_SUBSCRIBE;
                case 1:
                    return SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS;
                case 2:
                    return DONT_SEND_AT_SUBSCRIBE;
                default:
                    String errMsg = "invalid RetainedHandlingPolicy: " + value;
                    throw new IllegalArgumentException(errMsg);
            }
        }
    }

    private MqttQoS qos;
    private boolean noLocal;
    private boolean retainAsPublished;
    private RetainedHandlingPolicy retainHandling;

    public static MqttSubscriptionOption onlyFromQos(MqttQoS qos) {
        return new MqttSubscriptionOption(qos, false, false, RetainedHandlingPolicy.SEND_AT_SUBSCRIBE);
    }

    public MqttSubscriptionOption(MqttQoS qos,
        boolean noLocal,
        boolean retainAsPublished,
        RetainedHandlingPolicy retainHandling) {
        this.qos = qos;
        this.noLocal = noLocal;
        this.retainAsPublished = retainAsPublished;
        this.retainHandling = retainHandling;
    }

    public MqttQoS qos() {
        return qos;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    public boolean isRetainAsPublished() {
        return retainAsPublished;
    }

    public RetainedHandlingPolicy retainHandling() {
        return retainHandling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MqttSubscriptionOption that = (MqttSubscriptionOption)o;

        if (noLocal != that.noLocal) {
            return false;
        }
        if (retainAsPublished != that.retainAsPublished) {
            return false;
        }
        if (qos != that.qos) {
            return false;
        }
        return retainHandling == that.retainHandling;
    }

    @Override
    public int hashCode() {
        int result = qos.hashCode();
        result = 31 * result + (noLocal ? 1 : 0);
        result = 31 * result + (retainAsPublished ? 1 : 0);
        result = 31 * result + retainHandling.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubscriptionOption[" +
            "qos=" + qos +
            ", noLocal=" + noLocal +
            ", retainAsPublished=" + retainAsPublished +
            ", retainHandling=" + retainHandling +
            ']';
    }
}