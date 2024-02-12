package com.vraft.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/7 17:26
 **/
@Data
public class RpcChannel {
    private String key;
    private int maxSize;
    private final int groupSize = 8;
    private final Map<Long, Object>[] waitAck;
    private CopyOnWriteArrayList<Channel> chs;

    public RpcChannel(String key, int maxSize) {
        this.key = key;
        this.maxSize = maxSize;
        this.chs = new CopyOnWriteArrayList<>();
        this.waitAck = newAckGroup(groupSize);
    }

    private Map<Long, Object>[] newAckGroup(int size) {
        Map<Long, Object>[] group = new Map[size];
        for (int i = 0; i < size; i++) {
            group[i] = new ConcurrentHashMap<>();
        }
        return group;
    }

    private void addSuspendAck(long id, Object obj) {
        final int index = (int)(id & (groupSize - 1));
        waitAck[index].put(id, obj);
    }

    private Object removeSuspendAck(long id) {
        final int index = (int)(id & (groupSize - 1));
        return waitAck[index].remove(id);
    }

    private Object getSuspendAck(long id) {
        final int index = (int)(id & (groupSize - 1));
        return waitAck[index].get(id);
    }

}
