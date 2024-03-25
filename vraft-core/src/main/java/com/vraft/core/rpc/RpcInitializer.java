package com.vraft.core.rpc;

import java.net.InetSocketAddress;

import com.vraft.core.rpc.RpcCodec.Decoder;
import com.vraft.core.rpc.RpcCodec.Encoder;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcManager;
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
        private final RpcClientHandler handler;

        public ClientInitializer(SystemCtx sysCtx) {
            this.sysCtx = sysCtx;
            this.handler = new RpcClientHandler(sysCtx);
        }

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            final ChannelPipeline pipe = sc.pipeline();
            pipe.addLast(new Decoder());
            pipe.addLast(new Encoder());
            pipe.addLast(handler);
            pipe.addLast(RpcCommon.newIdleHandler(RpcConsts.CH_IDLE_MAX));
        }
    }

    public static class ServerInitializer
        extends ChannelInitializer<SocketChannel> {
        private final SystemCtx sysCtx;
        private final RpcServerHandler handler;

        public ServerInitializer(SystemCtx sysCtx) {
            this.sysCtx = sysCtx;
            this.handler = new RpcServerHandler(sysCtx);
        }

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            final ChannelPipeline pipe = sc.pipeline();
            pipe.addLast(new Decoder());
            pipe.addLast(new Encoder());
            pipe.addLast(handler);
            pipe.addLast(RpcCommon.newIdleHandler(RpcConsts.CH_IDLE_MAX));
        }
    }

    @Sharable
    public static class RpcServerHandler
        extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;

        public RpcServerHandler(SystemCtx sysCtx) {
            this.sysCtx = sysCtx;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            try {
                String host = RpcCommon.remoteAddressStr(ctx.channel());
                logger.info("rpc server active:{}", host);
                UidService uid = sysCtx.getUidSvs();
                RpcManager rpcMgr = sysCtx.getRpcMgr();
                rpcMgr.addChannel(uid.genUserId(), host, ctx.channel());
            } catch (Exception ex) {
                logger.error("rpc server active error:{}", ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            try {
                InetSocketAddress sad = RpcCommon.remoteAddress(ctx.channel());
                logger.info("rpc server inactive:{}", sad);
                RpcManager rpcMgr = sysCtx.getRpcMgr();
                rpcMgr.removeChannel(ctx.channel());
            } catch (Exception ex) {
                logger.error("rpc server inactive error:{}", ex.getMessage());
                ex.printStackTrace();
            }

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx,
            Object msg) throws Exception {
            if (!(msg instanceof ByteBuf)) {return;}
            ActorService actor = sysCtx.getActorSvs();
            final RpcManager rpcMgr = sysCtx.getRpcMgr();
            final ByteBuf bf = (ByteBuf)msg;
            try {
                long nodeId = RpcCommon.getNodeId(bf);
                long groupId = RpcCommon.getGroupId(bf);
                actor.dispatchRaftGroup(groupId, nodeId, bf.retain());
                //long userId = rpcMgr.getUserId(ctx.channel());
                //RpcCommon.setGroupId(bf, userId);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {bf.release();}
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
            logger.error(" rpc server exceptionCaught:{},{}",
                ctx.channel(), cause);
        }
    }

    @Sharable
    public static class RpcClientHandler
        extends ChannelDuplexHandler {
        private final SystemCtx sysCtx;

        public RpcClientHandler(SystemCtx sysCtx) {
            this.sysCtx = sysCtx;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            try {
                InetSocketAddress sad = RpcCommon.remoteAddress(ctx.channel());
                logger.info("rpc client active:{}", sad);
            } catch (Exception ex) {
                logger.error("rpc client active error:{}", ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            try {
                InetSocketAddress sad = RpcCommon.remoteAddress(ctx.channel());
                logger.info("rpc client inactive:{}", sad);
                RpcManager rpcMgr = sysCtx.getRpcMgr();
                rpcMgr.removeChannel(ctx.channel());
            } catch (Exception ex) {
                logger.error("rpc client inactive error:{}", ex.getMessage());
                ex.printStackTrace();
            }
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
            logger.error("rpc client exceptionCaught:{},{}",
                ctx.channel(), cause);
        }
    }

}
