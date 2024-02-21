package com.vraft.core.rpc;

import com.vraft.core.rpc.RpcInitializer.ServerInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcBuilder;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcServer;
import com.vraft.facade.system.SystemCtx;
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
public class RpcServerImpl implements RpcServer {
    private final static Logger logger = LogManager.getLogger(RpcServerImpl.class);

    private Channel channel;
    private final RpcBuilder bd;
    private final SystemCtx sysCtx;
    private EventLoopGroup boss, worker;

    public RpcServerImpl(SystemCtx sysCtx, RpcBuilder bd) {
        this.bd = bd;
        this.sysCtx = sysCtx;
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

    @Override
    public boolean oneWay(long userId, String uid, byte[] header,
        byte[] body) throws Exception {
        return RpcCommon.dispatchOneWay(sysCtx, userId, uid, header, body);
    }

    @Override
    public boolean twoWay(long userId, String uid, byte[] header,
        byte[] body, long timeout, CallBack cb) throws Exception {
        return RpcCommon.dispatchTwoWay(sysCtx, userId,
            uid, header, body, timeout, cb);
    }

    @Override
    public boolean resp(SystemCtx ctx, long userId, long msgId,
        String uid, byte[] header, byte[] body) throws Exception {
        return RpcCommon.dispatchResp(sysCtx, userId,
            msgId, uid, header, body);
    }

    private Channel newTcpServer(RpcBuilder bd) throws Exception {
        final ServerBootstrap b = new ServerBootstrap();
        boss = RpcCommon.eventLoop(bd.getBossNum());
        worker = RpcCommon.eventLoop(bd.getWorkerNum());
        setOpts(b);
        b.group(boss, worker);
        b.channel(RpcCommon.serverCls());
        b.childHandler(new ServerInitializer(sysCtx));
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
