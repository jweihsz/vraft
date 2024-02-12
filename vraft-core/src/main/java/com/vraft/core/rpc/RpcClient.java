package com.vraft.core.rpc;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.rpc.RpcConsts;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 22:37
 **/
public class RpcClient extends RpcAbstract implements LifeCycle {
    private final static Logger logger = LogManager.getLogger(RpcClient.class);

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private final RpcBuilder bd;

    private final Map<String, RpcChannel> pools;

    public RpcClient(RpcBuilder bd) {
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
        ch.attr(RpcCommon.CHANNEL_KEY).set(key);
        pools.get(key).getChs().add(ch);
    }

    public Channel getChannel(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        final RpcChannel chs = pools.get(key);
        if (chs == null || chs.getChs().isEmpty()) {
            return null;
        }
        int size = chs.getChs().size();
        int index = RpcCommon.random().nextInt(size);
        Channel ch = chs.getChs().get(index);
        return ch != null && ch.isActive() ? ch : null;
    }

    public boolean removeChannel(String key, Channel ch) {
        if (key == null || key.isEmpty() || ch == null) {
            return false;
        }
        final RpcChannel chs = pools.get(key);
        if (chs == null || chs.getChs().isEmpty()) {
            return false;
        }
        return chs.getChs().remove(ch);
    }

    public RpcChannel newChannelPool(String key, int max) {
        Objects.requireNonNull(key);
        if (max <= 0) {max = 1;}
        RpcChannel n = new RpcChannel(key, max);
        RpcChannel o = pools.putIfAbsent(key, n);
        return o == null ? n : o;
    }

    public void connect(Bootstrap bs, final String host, int size) {
        Objects.requireNonNull(bs);
        Objects.requireNonNull(host);
        RpcChannel pool = pools.get(host);
        Objects.requireNonNull(pool);
        int cur = pool.getChs().size();
        int df = pool.getMaxSize() - cur;
        int need = Math.min(df, size <= 0 ? 1 : size);
        for (int i = 0; i < need; i++) {
            doConnect(bs, host);
        }
    }

    private void doConnect(Bootstrap bs, String host) {
        InetSocketAddress a = RpcCommon.parser(host);
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

    private Bootstrap newTcpClient(RpcBuilder bd) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = RpcCommon.eventLoop(bd.getBossNum());
        b.group(group);
        b.channel(RpcCommon.serverCls());
        b.handler(new ClientInitializer(this));
        return b;
    }

}
