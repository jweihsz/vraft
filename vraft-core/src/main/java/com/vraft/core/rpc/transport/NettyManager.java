package com.vraft.core.rpc.transport;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * @author jweihsz
 * @version 2024/2/7 17:31
 **/
@NotThreadSafe
public class NettyManager {
    private final static Logger logger = LogManager.getLogger(NettyManager.class);

    private final Map<String, NettyChannel> pools;

    public NettyManager() {
        this.pools = new ConcurrentHashMap<>();
    }

    public void addChannel(String key, Channel ch) {
        Objects.requireNonNull(ch);
        Objects.requireNonNull(key);
        Objects.requireNonNull(pools.get(key));
        ch.attr(NettyCommon.CHANNEL_KEY).set(key);
        pools.get(key).getChs().add(ch);
    }

    public Channel getChannel(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        final NettyChannel chs = pools.get(key);
        if (chs == null || chs.getChs().isEmpty()) {
            return null;
        }
        int size = chs.getChs().size();
        int index = NettyCommon.random().nextInt(size);
        Channel ch = chs.getChs().get(index);
        return ch != null && ch.isActive() ? ch : null;
    }

    public boolean removeChannel(String key, Channel ch) {
        if (key == null || key.isEmpty() || ch == null) {
            return false;
        }
        final NettyChannel chs = pools.get(key);
        if (chs == null || chs.getChs().isEmpty()) {
            return false;
        }
        return chs.getChs().remove(ch);
    }

    public NettyChannel newChannelPool(String key, int max) {
        Objects.requireNonNull(key);
        if (max <= 0) {
            max = 1;
        }
        NettyChannel n = new NettyChannel(key, max);
        NettyChannel o = pools.putIfAbsent(key, n);
        return o == null ? n : o;
    }

    public void doConnect(Bootstrap bs, final String host, int size) {
        Objects.requireNonNull(bs);
        Objects.requireNonNull(host);
        NettyChannel pool = pools.get(host);
        Objects.requireNonNull(pool);
        int cur = pool.getChs().size();
        int df = pool.getMaxSize() - cur;
        int need = Math.min(df, size <= 0 ? 1 : size);
        InetSocketAddress a = NettyCommon.parser(host);
        for (int i = 0; i < need; i++) {
            final ChannelFuture future = bs.connect(a);
            future.addListener((fc) -> {
                if (!fc.isSuccess()) {
                    logger.error("connect {} fail!", host);
                } else {
                    addChannel(host, future.channel());
                    logger.info("connect {} success!", host);
                }
            });
        }
    }
}
