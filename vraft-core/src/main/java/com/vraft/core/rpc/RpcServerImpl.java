package com.vraft.core.rpc;

import java.util.concurrent.atomic.AtomicBoolean;

import com.vraft.core.rpc.RpcInitializer.ServerInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.config.RpcServerCfg;
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
    private final RpcServerCfg cfg;
    private final SystemCtx sysCtx;
    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private final AtomicBoolean start;

    public RpcServerImpl(SystemCtx sysCtx, RpcServerCfg cfg) {
        this.cfg = cfg;
        this.sysCtx = sysCtx;
        this.boss = RpcCommon.BOSS_GROUP;
        this.worker = RpcCommon.WORKER_GROUP;
        this.start = new AtomicBoolean(false);
    }

    @Override
    public void startup() throws Exception {
        if (!start.compareAndSet(false, true)) {return;}
        this.channel = newTcpServer(cfg);
        logger.info("rpc server start up:{}", channel.toString());
    }

    @Override
    public void shutdown() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
    }

    @Override
    public boolean oneWay(long userId, long msgId, byte biz, long groupId,
        long nodeId, String uid, byte[] header, byte[] body) throws Exception {
        return RpcCommon.dispatchOneWay(
            sysCtx, userId, msgId, biz, groupId, nodeId, uid, header, body);
    }

    @Override
    public boolean twoWay(long userId, byte biz, long groupId,
        long nodeId, String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception {
        return RpcCommon.dispatchTwoWay(
            sysCtx, userId, biz, groupId, nodeId,
            uid, header, body, timeout, cb);
    }

    @Override
    public boolean resp(long userId, byte biz, long groupId,
        long nodeId, long msgId, String uid, byte[] header, byte[] body) throws Exception {
        return RpcCommon.dispatchResp(
            sysCtx, userId, biz, groupId, nodeId,
            msgId, uid, header, body);
    }

    private Channel newTcpServer(RpcServerCfg cfg) throws Exception {
        final ServerBootstrap b = new ServerBootstrap();
        setOpts(b, cfg);
        b.group(boss, worker);
        b.channel(RpcCommon.serverCls());
        b.childHandler(new ServerInitializer(sysCtx));
        return b.bind(cfg.getRpcSrvHost(), cfg.getRpcSrvPort()).sync().channel();
    }

    private void setOpts(ServerBootstrap b, RpcServerCfg cfg) {
        b.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        b.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        b.childOption(ChannelOption.SO_RCVBUF, cfg.getRpcSrvRcvBufSize());
        b.childOption(ChannelOption.SO_SNDBUF, cfg.getRpcSrvSndBufSize());
    }
}
