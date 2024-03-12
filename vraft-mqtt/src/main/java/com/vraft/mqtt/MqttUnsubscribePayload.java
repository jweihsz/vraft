package com.vraft.mqtt;

import java.util.ArrayList;
import java.util.List;

import io.netty.util.internal.StringUtil;

/**
 * @author jweihsz
 * @version 2024/3/11 21:07
 **/
public class MqttUnsubscribePayload {
    private List<String> topics;
    private int numberOfBytesConsumed;

    public MqttUnsubscribePayload() {
        this.topics = new ArrayList<>();
    }

    public void add(String topic) {
        this.topics.add(topic);
    }

    public void recycle() {
        this.topics.clear();
    }

    public List<String> topics() {return topics;}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(StringUtil.simpleClassName(this)).append('[');
        for (int i = 0; i < topics.size(); i++) {
            builder.append("topicName = ").append(topics.get(i)).append(", ");
        }
        if (!topics.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]").toString();
    }
}