package com.vraft.mqtt;

import io.netty.util.internal.StringUtil;

/**
 * @author jweihsz
 * @version 2024/3/11 20:52
 **/
public class MqttTopicSubscription {
    private final String topicFilter;
    private final MqttSubscriptionOption option;

    public MqttTopicSubscription(String topicFilter, MqttQoS qualityOfService) {
        this.topicFilter = topicFilter;
        this.option = MqttSubscriptionOption.onlyFromQos(qualityOfService);
    }

    public MqttTopicSubscription(String topicFilter, MqttSubscriptionOption option) {
        this.topicFilter = topicFilter;
        this.option = option;
    }
    
    public String topicFilter() {
        return topicFilter;
    }

    public MqttQoS qualityOfService() {
        return option.qos();
    }

    public MqttSubscriptionOption option() {
        return option;
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
            .append('[')
            .append("topicFilter=").append(topicFilter)
            .append(", option=").append(this.option)
            .append(']')
            .toString();
    }
}
