package com.vraft.core.rpc;

import com.vraft.core.rpc.RpcCodec.Decoder;
import com.vraft.core.rpc.RpcCodec.Encoder;
import com.vraft.facade.rpc.RpcClient;
import com.vraft.facade.rpc.RpcServer;
import com.vraft.facade.system.SystemCtx;
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

    public static class ClientInitializer extends ChannelInitializer<SocketChannel> {
        private final SystemCtx sysCtx;
        private final RpcClient client;
        private final RpcClientHandler handler;

        public ClientInitializer(SystemCtx sysCtx, RpcClient client) {
            this.sysCtx = sysCtx;
            this.client = client;
            this.handler = new RpcClientHandler(sysCtx, client);
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

    public static class ServerInitializer extends ChannelInitializer<SocketChannel> {
        private final SystemCtx sysCtx;
        private final RpcServer rpcServer;
        private final RpcServerHandler handler;

        public ServerInitializer(SystemCtx sysCtx, RpcServer rpcServer) {
            this.sysCtx = sysCtx;
            this.rpcServer = rpcServer;
            this.handler = new RpcServerHandler(sysCtx, rpcServer);
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
    public static class RpcServerHandler extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;
        private final RpcServer rpcServer;

        public RpcServerHandler(SystemCtx sysCtx, RpcServer rpcServer) {
            this.sysCtx = sysCtx;
            this.rpcServer = rpcServer;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel active:{}", RpcCommon.remoteAddress(ctx.channel()));
            rpcServer.registerUserId(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel inactive:{}", RpcCommon.remoteAddress(ctx.channel()));
            rpcServer.unregisterUserId(ctx.channel());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof ByteBuf)) {return;}
            final ByteBuf bf = (ByteBuf)msg;
            ByteBuf uid = RpcCommon.getRpcUid(bf);
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            //
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        }
    }

    @Sharable
    public static class RpcClientHandler extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;
        private final RpcClient client;

        public RpcClientHandler(SystemCtx sysCtx, RpcClient client) {
            this.sysCtx = sysCtx;
            this.client = client;

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel active:{}", RpcCommon.remoteAddress(ctx.channel()));
            client.registerUserId(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channel inactive:{}", RpcCommon.remoteAddress(ctx.channel()));
            client.unregisterUserId(ctx.channel());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            //
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        }
    }

}
