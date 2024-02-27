package com.vraft.core.rpc;

import java.net.InetSocketAddress;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.config.CfgRpcNode;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.system.SystemCtx;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 22:37
 **/
public class RpcClientImpl implements RpcClient {
    private final static Logger logger = LogManager.getLogger(RpcClientImpl.class);

    private Bootstrap bs;
    private EventLoopGroup group;
    private final CfgRpcNode cfg;
    private final SystemCtx sysCtx;

    public RpcClientImpl(SystemCtx sysCtx, CfgRpcNode cfg) {
        this.cfg = cfg;
        this.sysCtx = sysCtx;
    }

    @Override
    public void startup() throws Exception {
        this.bs = newTcpClient(cfg);
    }

    @Override
    public void shutdown() {}

    @Override
    public Object doConnect(String host) throws Exception {
        InetSocketAddress a = RpcCommon.parser(host);
        final ChannelFuture future = bs.connect(a);
        return future.awaitUninterruptibly(3000) ? future.channel() : null;
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

    private Bootstrap newTcpClient(CfgRpcNode cfg) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = RpcCommon.WORKER_GROUP;
        b.group(group);
        b.channel(RpcCommon.clientCls());
        b.option(ChannelOption.SO_RCVBUF, cfg.getRpcRcvBufSize());
        b.option(ChannelOption.SO_SNDBUF, cfg.getRpcSndBufSize());
        b.handler(new ClientInitializer(sysCtx));
        return b;
    }
}
