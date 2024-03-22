package com.vraft.core.rpc;

import java.net.InetSocketAddress;

import com.vraft.core.rpc.RpcInitializer.ClientInitializer;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.config.RpcClientCfg;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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
    private final RpcClientCfg cfg;
    private final SystemCtx sysCtx;

    public RpcClientImpl(SystemCtx sysCtx, RpcClientCfg cfg) {
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
    public long doConnect(String host) {
        try {
            RpcManager rpcMgr = sysCtx.getRpcMgr();
            long userId = rpcMgr.getUserId(host);
            if (userId > 0) {return userId;}
            InetSocketAddress a = RpcCommon.parser(host);
            final ChannelFuture future = bs.connect(a);
            final int timeout = RpcCommon.CONN_TIMEOUT;
            future.awaitUninterruptibly(timeout);
            if (!future.isDone()) {
                String errMsg = "Connect " + host + " timeout!";
                logger.warn(errMsg);
                throw new Exception(errMsg);
            }
            if (future.isCancelled()) {
                String errMsg = "Connect " + host + " cancelled!";
                logger.warn(errMsg);
                throw new Exception(errMsg);
            }
            if (!future.isSuccess()) {
                String errMsg = "Connect " + host + " error!";
                logger.warn(errMsg);
                throw new Exception(errMsg, future.cause());
            }
            Channel ch = future.channel();
            if (ch == null) {return -1L;}
            UidService uid = sysCtx.getUidSvs();
            userId = uid.genUserId();
            rpcMgr.addChannel(userId, host, ch);
            return userId;
        } catch (Exception ex) {return -1L;}
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

    private Bootstrap newTcpClient(RpcClientCfg cfg) throws Exception {
        final Bootstrap b = new Bootstrap();
        group = RpcCommon.WORKER_GROUP;
        b.group(group);
        b.channel(RpcCommon.clientCls());
        b.option(ChannelOption.SO_RCVBUF, cfg.getRpcClientRcvBufSize());
        b.option(ChannelOption.SO_SNDBUF, cfg.getRpcClientSndBufSize());
        b.handler(new ClientInitializer(sysCtx));
        return b;
    }

}
