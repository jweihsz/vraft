package com.vraft.core.rpc;

import java.net.InetSocketAddress;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.rpc.RpcBuilder;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
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
public class RpcClientImpl implements RpcClient {
    private final static Logger logger = LogManager.getLogger(RpcClientImpl.class);

    private Bootstrap bs;
    private EventLoopGroup group;
    private final RpcBuilder bd;
    private final SystemCtx sysCtx;
    private final RpcManager rpcMgr;
    private final RpcHelper rpcHelper;

    public RpcClientImpl(SystemCtx sysCtx, RpcBuilder bd) {
        this.bd = bd;
        this.sysCtx = sysCtx;
        this.rpcMgr = new RpcManager(sysCtx);
        this.rpcHelper = new RpcHelper(sysCtx, rpcMgr);
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
    public boolean registerUserId(Object ch) {
        if (ch == null) {return false;}
        if (!(ch instanceof Channel)) {return false;}
        UidService uid = sysCtx.getUidService();
        long userId = uid.genUserId();
        long actorId = uid.genActorId();
        rpcMgr.addChannel(userId, actorId, (Channel)ch);
        return true;
    }

    @Override
    public boolean unregisterUserId(Object ch) {
        if (ch == null) {return false;}
        if (!(ch instanceof Channel)) {return false;}
        rpcMgr.removeChannel((Channel)ch);
        return true;
    }

    @Override
    public void unregisterProcessor(String uid) {
        rpcHelper.unregisterProcessor(uid);
    }

    @Override
    public RpcProcessor<?> getProcessor(Object uid) {
        return rpcHelper.getProcessor(uid);
    }

    @Override
    public void registerProcessor(String uid, RpcProcessor<?> processor) {
        rpcHelper.registerProcessor(uid, processor);
    }

    @Override
    public Object doConnect(String host) throws Exception {
        InetSocketAddress a = RpcCommon.parser(host);
        final ChannelFuture future = bs.connect(a);
        return future.awaitUninterruptibly(3000) ? future.channel() : null;
    }

    private Bootstrap newTcpClient(RpcBuilder bd) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = RpcCommon.eventLoop(bd.getBossNum());
        b.group(group);
        b.channel(RpcCommon.serverCls());
        b.handler(new ClientInitializer(sysCtx, this));
        return b;
    }

}
