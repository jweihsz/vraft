package com.vraft.core.rpc;

import java.net.InetSocketAddress;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcBuilder;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.system.SystemCtx;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
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
    private final RpcBuilder bd;
    private final SystemCtx sysCtx;

    public RpcClientImpl(SystemCtx sysCtx, RpcBuilder bd) {
        this.bd = bd;
        this.sysCtx = sysCtx;
    }

    @Override
    public void startup() throws Exception {
        if (bd.getWire() == RpcConsts.TCP) {
            this.bs = newTcpClient(bd);
        } else if (bd.getWire() == RpcConsts.UDP) {
            this.bs = null;
        }
    }

    @Override
    public void shutdown() {
        if (group != null) {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Override
    public Object doConnect(String host) throws Exception {
        InetSocketAddress a = RpcCommon.parser(host);
        final ChannelFuture future = bs.connect(a);
        return future.awaitUninterruptibly(3000) ? future.channel() : null;
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

    private Bootstrap newTcpClient(RpcBuilder bd) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = RpcCommon.eventLoop(bd.getBossNum());
        b.group(group);
        b.channel(RpcCommon.serverCls());
        b.handler(new ClientInitializer(sysCtx));
        return b;
    }
}
