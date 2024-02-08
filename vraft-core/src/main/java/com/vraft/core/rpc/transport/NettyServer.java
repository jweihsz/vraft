package com.vraft.core.rpc.transport;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vraft.facade.common.LifeCycle;
import com.vraft.facade.rpc.RpcConsts;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;

/**
 * @author jweihsz
 * @version 2024/2/8 22:37
 **/
public class NettyServer implements LifeCycle {
    private final static Logger logger = LogManager.getLogger(NettyServer.class);

    private Channel channel;
    private final NettyBuilder bd;
    private EventLoopGroup boss, worker;

    public NettyServer(NettyBuilder bd) {
        this.bd = bd;
        check(bd);
    }

    @Override
    public void startup() throws Exception {
        if (bd.getWire() == RpcConsts.TCP) {
            this.channel = newTcpChannel(bd);
        } else if (bd.getWire() == RpcConsts.UDP) {
            this.channel = null;
        }
    }

    @Override
    public void shutdown() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
        if (boss != null) {
            boss.shutdownGracefully().syncUninterruptibly();
        }
        if (worker != null) {
            worker.shutdownGracefully().syncUninterruptibly();
        }
    }

    private Channel newTcpChannel(NettyBuilder bd) throws Exception {
        final ServerBootstrap b = new ServerBootstrap();
        boss = NettyCommon.eventLoop(bd.getBossNum());
        worker = NettyCommon.eventLoop(bd.getWorkerNum());
        b.group(boss, worker);
        b.channel(NettyCommon.serverCls());
        b.childHandler(bd.getInitializer());
        if (bd.getOpts() != null && !bd.getOpts().isEmpty()) {
            setOpts(b, bd.getOpts());
        }
        if (bd.getChildOpts() != null && !bd.getChildOpts().isEmpty()) {
            setChildOpts(b, bd.getChildOpts());
        }
        return b.bind(bd.getHost(), bd.getPort()).sync().channel();
    }

    private void check(NettyBuilder nbd) {
        if (nbd.getType() != RpcConsts.SERVER) {
            logger.error("Not Server Type");
            throw new RuntimeException();
        }
        if (nbd.getHost() == null) {
            logger.error("Host empty");
            throw new RuntimeException();
        }
    }

    private void setOpts(ServerBootstrap b, Map<ChannelOption<?>, Object> opts) {
        for (Map.Entry<ChannelOption<?>, Object> e : opts.entrySet()) {
            b.option((ChannelOption<? super Object>)e.getKey(), e.getValue());
        }
    }

    private void setChildOpts(ServerBootstrap b, Map<ChannelOption<?>, Object> opts) {
        for (Map.Entry<ChannelOption<?>, Object> e : opts.entrySet()) {
            b.childOption((ChannelOption<? super Object>)e.getKey(), e.getValue());
        }
    }

}
