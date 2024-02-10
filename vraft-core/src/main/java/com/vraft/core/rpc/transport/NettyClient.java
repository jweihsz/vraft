package com.vraft.core.rpc.transport;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.rpc.RpcConsts;

import io.netty.bootstrap.Bootstrap;
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

    public NettyClient(NettyBuilder bd) {
        this.bd = bd;
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
