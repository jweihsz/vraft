package com.vraft.core.rpc;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.rpc.RpcBuilder;
import com.vraft.facade.rpc.RpcClient;
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
public class RpcClientImpl extends RpcAbstract implements RpcClient {
    private final static Logger logger = LogManager.getLogger(RpcClientImpl.class);

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private final RpcBuilder bd;

    private final Map<String, Channel> pools;

    public RpcClientImpl(RpcBuilder bd) {
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

    private void doConnect(Bootstrap bs, String host) {
        InetSocketAddress a = RpcCommon.parser(host);
        final ChannelFuture future = bs.connect(a);
        future.addListener((fc) -> {
            if (!fc.isSuccess()) {
                logger.error("connect {} fail!", host);
            } else {
                pools.put(host, future.channel());
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
