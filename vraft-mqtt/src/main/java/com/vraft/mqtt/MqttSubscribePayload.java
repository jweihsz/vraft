package com.vraft.mqtt;

import java.util.ArrayList;
import java.util.List;

import io.netty.util.internal.StringUtil;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/11 20:54
 **/
@Data
public class MqttSubscribePayload {
    private int numberOfBytesConsumed;
    private List<MqttTopicSubscription> topicSubscriptions;

    public MqttSubscribePayload() {
        this.topicSubscriptions = new ArrayList<>();
    }

    public void add(MqttTopicSubscription subscription) {
        this.topicSubscriptions.add(subscription);
    }

    public void rest(MqttTopicSubscription subscription) {
        this.topicSubscriptions.clear();
    }

    public List<MqttTopicSubscription> topicSubscriptions() {
        return topicSubscriptions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(StringUtil.simpleClassName(this)).append('[');
        for (int i = 0; i < topicSubscriptions.size(); i++) {
            builder.append(topicSubscriptions.get(i)).append(", ");
        }
        if (!topicSubscriptions.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        return builder.append(']').toString();
    }
}
