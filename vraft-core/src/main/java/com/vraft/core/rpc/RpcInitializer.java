package com.vraft.core.rpc;

import com.vraft.core.rpc.RpcCodec.Decoder;
import com.vraft.core.rpc.RpcCodec.Encoder;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/12 10:02
 **/
public class RpcInitializer {
    private final static Logger logger = LogManager.getLogger(RpcInitializer.class);

    public static class ClientInitializer
        extends ChannelInitializer<SocketChannel> {
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;
        private final RpcHelper rpcHelper;
        private final RpcClientHandler handler;

        public ClientInitializer(SystemCtx sysCtx,
            RpcManager rpcMgr, RpcHelper rpcHelper) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
            this.rpcHelper = rpcHelper;
            this.handler = new RpcClientHandler(sysCtx, rpcMgr, rpcHelper);
        }

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            final ChannelPipeline pipe = sc.pipeline();
            pipe.addLast(new Decoder());
            pipe.addLast(new Encoder());
            pipe.addLast(handler);
            pipe.addLast(RpcCommon.newIdleHandler(120));
        }
    }

    public static class ServerInitializer
        extends ChannelInitializer<SocketChannel> {
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;
        private final RpcHelper rpcHelper;
        private final RpcServerHandler handler;

        public ServerInitializer(SystemCtx sysCtx,
            RpcManager rpcMgr, RpcHelper rpcHelper) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
            this.rpcHelper = rpcHelper;
            this.handler = new RpcServerHandler(sysCtx, rpcMgr, rpcHelper);
        }

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            final ChannelPipeline pipe = sc.pipeline();
            pipe.addLast(new Decoder());
            pipe.addLast(new Encoder());
            pipe.addLast(handler);
            pipe.addLast(RpcCommon.newIdleHandler(120));
        }
    }

    @Sharable
    public static class RpcServerHandler
        extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;
        private final RpcHelper rpcHelper;

        public RpcServerHandler(SystemCtx sysCtx,
            RpcManager rpcMgr, RpcHelper rpcHelper) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
            this.rpcHelper = rpcHelper;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel active:{}",
                RpcCommon.remoteAddress(ctx.channel()));
            UidService uid = sysCtx.getUidService();
            long userId = uid.genUserId();
            rpcMgr.addChannel(userId, ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel inactive:{}",
                RpcCommon.remoteAddress(ctx.channel()));
            rpcMgr.removeChannel(ctx.channel());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx,
            Object msg) throws Exception {
            if (!(msg instanceof ByteBuf)) {return;}
            final ByteBuf bf = (ByteBuf)msg;
            ByteBuf uid = RpcCommon.getRpcUid(bf);
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx,
            Object evt) throws Exception {
            //
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
            Throwable cause) throws Exception {

        }
    }

    @Sharable
    public static class RpcClientHandler
        extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;
        private final RpcHelper rpcHelper;

        public RpcClientHandler(SystemCtx sysCtx,
            RpcManager rpcMgr, RpcHelper rpcHelper) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
            this.rpcHelper = rpcHelper;

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel active:{}",
                RpcCommon.remoteAddress(ctx.channel()));
            UidService uid = sysCtx.getUidService();
            long userId = uid.genUserId();
            rpcMgr.addChannel(userId, ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel inactive:{}",
                RpcCommon.remoteAddress(ctx.channel()));
            rpcMgr.removeChannel(ctx.channel());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx,
            Object msg) throws Exception {
            //
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx,
            Object evt) throws Exception {
            //
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
            Throwable cause) throws Exception {

        }
    }

}
