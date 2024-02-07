package com.vraft.core.rpc.transport;

import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/2/7 17:26
 **/
@Data
public class NettyChannel {
    private String key;
    private int maxSize;
    private CopyOnWriteArrayList<Channel> chs;

    public NettyChannel(String key, int maxSize) {
        this.key = key;
        this.maxSize = maxSize;
        this.chs = new CopyOnWriteArrayList<>();
    }
}
