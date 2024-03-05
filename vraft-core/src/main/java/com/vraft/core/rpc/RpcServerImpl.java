package com.vraft.core.rpc;

import com.vraft.core.rpc.RpcInitializer.ServerInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.config.RpcNodeCfg;
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
    private final RpcNodeCfg cfg;
    private final SystemCtx sysCtx;
    private final EventLoopGroup boss;
    private final EventLoopGroup worker;

    public RpcServerImpl(SystemCtx sysCtx, RpcNodeCfg cfg) {
        this.cfg = cfg;
        this.sysCtx = sysCtx;
        this.boss = RpcCommon.BOSS_GROUP;
        this.worker = RpcCommon.WORKER_GROUP;
    }

    @Override
    public void startup() throws Exception {
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
    public boolean oneWay(long userId, byte biz, String uid, byte[] header,
        byte[] body) throws Exception {
        return RpcCommon.dispatchOneWay(sysCtx, userId, biz, uid, header, body);
    }

    @Override
    public boolean twoWay(long userId, byte biz, String uid, byte[] header,
        byte[] body, long timeout, CallBack cb) throws Exception {
        return RpcCommon.dispatchTwoWay(sysCtx, userId, biz,
            uid, header, body, timeout, cb);
    }

    @Override
    public boolean resp(long userId, byte biz, long msgId,
        String uid, byte[] header, byte[] body) throws Exception {
        return RpcCommon.dispatchResp(sysCtx, userId, biz,
            msgId, uid, header, body);
    }

    private Channel newTcpServer(RpcNodeCfg cfg) throws Exception {
        final ServerBootstrap b = new ServerBootstrap();
        setOpts(b, cfg);
        b.group(boss, worker);
        b.channel(RpcCommon.serverCls());
        b.childHandler(new ServerInitializer(sysCtx));
        return b.bind(cfg.getRpcHost(), cfg.getRpcPort()).sync().channel();
    }

    private void setOpts(ServerBootstrap b, RpcNodeCfg cfg) {
        b.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        b.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        b.childOption(ChannelOption.SO_RCVBUF, cfg.getRpcRcvBufSize());
        b.childOption(ChannelOption.SO_SNDBUF, cfg.getRpcSndBufSize());
    }
}
