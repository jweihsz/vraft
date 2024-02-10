package com.vraft.core.rpc.transport;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.rpc.RpcConsts;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;

/**
 * @author jweihsz
 * @version 2024/2/8 22:37
 **/
public class NettyClient implements LifeCycle {
    private final static Logger logger = LogManager.getLogger(NettyClient.class);

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private final NettyBuilder bd;

    private final Map<String, NettyChannel> pools;

    public NettyClient(NettyBuilder bd) {
        this.bd = bd;
        this.pools = new ConcurrentHashMap<>();
    }

    @Override
    public void startup() throws Exception {
        if (bd.getWire() == RpcConsts.TCP) {
            this.bootstrap = newTcpClient(bd);
        } else if (bd.getWire() == RpcConsts.UDP) {
            this.bootstrap = null;
        }
    }

    @Override
    public void shutdown() {
        if (group != null) {
            group.shutdownGracefully().syncUninterruptibly();
        }
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

    private Bootstrap newTcpClient(NettyBuilder bd) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = NettyCommon.eventLoop(bd.getBossNum());
        b.group(group);
        b.channel(NettyCommon.serverCls());
        b.handler(bd.getInitializer());
        if (bd.getOpts() != null && !bd.getOpts().isEmpty()) {
            setOpts(b, bd.getOpts());
        }
        return b;
    }

    private void setOpts(Bootstrap b, Map<ChannelOption<?>, Object> opts) {
        for (Map.Entry<ChannelOption<?>, Object> e : opts.entrySet()) {
            b.option((ChannelOption<? super Object>)e.getKey(), e.getValue());
        }
    }

}
