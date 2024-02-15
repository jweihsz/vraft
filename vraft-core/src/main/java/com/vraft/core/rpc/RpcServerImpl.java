package com.vraft.core.rpc;

import com.vraft.core.rpc.RpcInitializer.ServerInitializer;
import com.vraft.facade.rpc.RpcBuilder;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 22:37
 **/
public class RpcServerImpl extends RpcAbstract implements RpcServer {
    private final static Logger logger = LogManager.getLogger(RpcServerImpl.class);

    private Channel channel;
    private final RpcBuilder bd;
    private EventLoopGroup boss, worker;

    public RpcServerImpl(RpcBuilder bd) {
        this.bd = bd;
        check(bd);
    }

    @Override
    public void startup() throws Exception {
        if (bd.getWire() == RpcConsts.TCP) {
            this.channel = newTcpServer(bd);
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

    private Channel newTcpServer(RpcBuilder bd) throws Exception {
        final ServerBootstrap b = new ServerBootstrap();
        boss = RpcCommon.eventLoop(bd.getBossNum());
        worker = RpcCommon.eventLoop(bd.getWorkerNum());
        setOpts(b);
        b.group(boss, worker);
        b.channel(RpcCommon.serverCls());
        b.childHandler(new ServerInitializer(this));
        return b.bind(bd.getHost(), bd.getPort()).sync().channel();
    }

    private void setOpts(ServerBootstrap b) {
        b.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        b.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        b.childOption(ChannelOption.SO_RCVBUF, 1024 * 1024);
        b.childOption(ChannelOption.SO_SNDBUF, 1024 * 1024);
    }

    private void check(RpcBuilder nbd) {
        if (nbd.getType() != RpcConsts.SERVER) {
            logger.error("Not Server Type");
            throw new RuntimeException();
        }
        if (nbd.getHost() == null) {
            logger.error("Host empty");
            throw new RuntimeException();
        }
    }
}
