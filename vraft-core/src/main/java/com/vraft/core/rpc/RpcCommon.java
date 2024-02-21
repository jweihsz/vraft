package com.vraft.core.rpc;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcCmd;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ThreadLocalRandom;

/**
 * @author jweihsz
 * @version 2024/2/6 16:51
 **/
public class RpcCommon {
    private RpcCommon() {}

    /* version(byte)|biz(byte)|type(byte)|seq(long)|uid-size(int)|
     * header-size(int)|body-size(int)|uid-content(bytes)|
     * header-content(bytes)|body-content(bytes)
     */
    public static int RPC_MATE_SIZE = 1 + 1 + 1 + 8 + 4 + 4 + 4;

    public static final byte[] EMPTY_BUFFER = new byte[0];

    public static final AttributeKey<Long> CH_KEY;
    public static final AttributeKey<Map<Long, Object>> CH_PEND;

    static {
        CH_KEY = AttributeKey.valueOf("ch_key");
        CH_PEND = AttributeKey.valueOf("ch_resp_pend");
    }

    public static Class<? extends SocketChannel> clientCls() {
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }

    public static Class<? extends ServerSocketChannel> serverCls() {
        if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueServerSocketChannel.class;
        } else {
            return NioServerSocketChannel.class;
        }
    }

    public static EventLoopGroup eventLoop(int nThreads) {
        if (Epoll.isAvailable()) {
            EpollEventLoopGroup epoll = null;
            epoll = new EpollEventLoopGroup(nThreads);
            return epoll;
        } else if (KQueue.isAvailable()) {
            KQueueEventLoopGroup kqueue = null;
            kqueue = new KQueueEventLoopGroup(nThreads);
            return kqueue;
        } else {
            NioEventLoopGroup nio = null;
            nio = new NioEventLoopGroup(nThreads);
            return nio;
        }
    }

    public static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    public static InetSocketAddress parser(String address) {
        Objects.requireNonNull(address);
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new RuntimeException();
        }
        return new InetSocketAddress(arr[0], Integer.parseInt(arr[1]));
    }

    public static InetSocketAddress localAddress(Channel ch) {
        return (InetSocketAddress)ch.localAddress();
    }

    public static InetSocketAddress remoteAddress(Channel ch) {
        return (InetSocketAddress)ch.remoteAddress();
    }

    public static IdleStateHandler newIdleHandler(int idle) {
        return new IdleStateHandler(0, 0, idle);
    }

    public static long getUserId(Channel ch) {
        if (ch == null) {return -1L;}
        return ch.attr(RpcCommon.CH_KEY).get();
    }

    public static ByteBuf convert(String str) {
        return Unpooled.copiedBuffer(str, CharsetUtil.UTF_8);
    }

    public static boolean checkRpcMate(ByteBuf bf) {
        int total = bf.readableBytes();
        return total >= RPC_MATE_SIZE;
    }

    public static int getRpcUidSize(ByteBuf bf) {
        return bf.getInt(9/*1+1+8-1*/);
    }

    public static int getRpcUidIndex(ByteBuf bf) {
        return RPC_MATE_SIZE - 1;
    }

    public static int getRpcHeaderSize(ByteBuf bf) {
        return bf.getInt(13/*1+1+8+4-1*/);
    }

    public static int getRpcHeaderIndex(ByteBuf bf) {
        return RPC_MATE_SIZE - 1 + getRpcUidSize(bf);
    }

    public static int getRpcBodySize(ByteBuf bf) {
        return bf.getInt(17/*1+1+8+4+4-1*/);
    }

    public static int getRpcBodyIndex(ByteBuf bf) {
        return RPC_MATE_SIZE - 1
            + getRpcUidSize(bf) + getRpcHeaderSize(bf);
    }

    public static byte getRpcVer(ByteBuf bf) {
        return bf.getByte(0);
    }

    public static int getRpcRq(ByteBuf bf) {
        return bf.getByte(1) & 0x03;
    }

    public static int getRpcType(ByteBuf bf) {
        return bf.getByte(1) & 0xFC >> 2;
    }

    public static long getRpcSeq(ByteBuf bf) {
        return bf.getLong(2);
    }

    public static ByteBuf getRpcUid(ByteBuf bf) {
        final int size = getRpcUidSize(bf);
        final int index = getRpcUidIndex(bf);
        return bf.slice(index, size);
    }

    public static ByteBuf getRpcHeader(ByteBuf bf) {
        final int size = getRpcHeaderSize(bf);
        final int index = getRpcHeaderIndex(bf);
        return bf.slice(index, size);
    }

    public static ByteBuf getRpcBody(ByteBuf bf) {
        final int size = getRpcBodySize(bf);
        final int index = getRpcBodyIndex(bf);
        return bf.slice(index, size);
    }

    public static boolean dispatchRpc(SystemCtx ctx, long userId,
        byte biz, byte type, long msgId, String uid, byte[] header,
        byte[] body, long timeout, CallBack cb) throws Exception {
        ActorService actor = ctx.getActorService();
        final RpcCmd cmd = buildBaseCmd(userId, biz, type,
            msgId, uid, body, header, cb, timeout);
        if (actor.dispatchWriteChMsg(userId, cmd)) {return true;}
        ((RpcCmdExt)cmd).recycle();
        return false;
    }

    public static boolean dispatchOneWay(SystemCtx ctx, long userId,
        byte biz, String uid, byte[] header, byte[] body) throws Exception {
        final UidService genId = ctx.getUidService();
        return dispatchRpc(ctx, userId, RpcConsts.RPC_ONE_WAY, biz,
            genId.genMsgId(), uid, header, body, -1L, null);
    }

    public static boolean dispatchTwoWay(SystemCtx ctx, long userId,
        byte biz, String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception {
        final UidService genId = ctx.getUidService();
        return dispatchRpc(ctx, userId, biz, RpcConsts.RPC_TWO_WAY,
            genId.genMsgId(), uid, header, body, timeout, cb);
    }

    public static boolean dispatchResp(SystemCtx ctx, long userId,
        byte biz, long msgId, String uid, byte[] header,
        byte[] body) throws Exception {
        return dispatchRpc(ctx, userId, biz, RpcConsts.RPC_RESPONSE,
            msgId, uid, header, body, -1L, null);
    }

    public static boolean invokeBatch(SystemCtx ctx, long userId,
        Consumer<Object> apply, List<RpcCmd> nodes) {
        CompositeByteBuf cbf = null;
        final RpcManager mgr = ctx.getRpcManager();
        Channel ch = (Channel)mgr.getChannel(userId);
        if (ch == null || !ch.isWritable()) {return false;}
        if (nodes == null || nodes.isEmpty()) {return false;}
        cbf = Unpooled.compositeBuffer(nodes.size());
        int size = 0;
        for (RpcCmd cmd : nodes) {
            ByteBuf bf = processRpcCmd(ctx, apply, cmd);
            if (bf == null) {continue;}
            cbf.addComponent(true, bf);
            size += 1;
        }
        if (size <= 0) {
            cbf.release();
            return false;
        } else {
            ch.writeAndFlush(cbf);
            return true;
        }
    }

    public static RpcCmd buildBaseCmd(long userId, byte biz,
        byte type, long id, String uid, byte[] body, byte[] header,
        CallBack cb, long timeout) {
        RpcCmd cmd = ObjectsPool.RPC_CMD_RECYCLER.get();
        cmd.setBiz(biz);
        cmd.setCallBack(cb);
        cmd.setMsgId(id);
        cmd.setType(type);
        cmd.setUserId(userId);
        cmd.setUid(uid);
        cmd.setHeader(header);
        cmd.setBody(body);
        cmd.setTimeout(timeout);
        cmd.setMsgId(id);
        return cmd;
    }

    public static ByteBuf processRpcCmd(SystemCtx ctx,
        Consumer<Object> apply, RpcCmd cmd) {
        if (RpcConsts.isOneWay(cmd.getType())) {
            return buildOneWayPkg(cmd);
        } else if (RpcConsts.isResp(cmd.getType())) {
            return buildOneWayPkg(cmd);
        } else if (RpcConsts.isTwoWay(cmd.getType())) {
            return buildTwoWayPkg(ctx, apply, cmd);
        }
        return null;
    }

    public static ByteBuf buildOneWayPkg(RpcCmd cmd) {
        return buildBasePkg(
            cmd.getType(),
            cmd.getMsgId(),
            cmd.getUid(),
            cmd.getHeader(),
            cmd.getBody());
    }

    private static ByteBuf buildTwoWayPkg(SystemCtx ctx,
        Consumer<Object> apply, RpcCmd cmd) {
        ByteBuf bf = buildBasePkg(cmd.getType(), cmd.getMsgId(),
            cmd.getUid(), cmd.getHeader(), cmd.getBody());
        final RpcManager mgr = ctx.getRpcManager();
        TimerService timer = ctx.getTimerService();
        Object task = timer.addTimeout(apply, cmd, cmd.getTimeout());
        if (task == null) {
            bf.release();
            ((RpcCmdExt)cmd).recycle();
            return null;
        } else {
            cmd.setExt(task);
            mgr.addPendMsg(cmd.getUserId(), cmd.getMsgId(), cmd);
            return bf;
        }
    }

    public static ByteBuf buildBasePkg(byte rq, long id,
        String uid, byte[] header, byte[] body) {
        int totalLen = RpcCommon.RPC_MATE_SIZE;
        byte[] bodyBuf, uidBuf, headerBuf;
        if (body == null) {
            bodyBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            bodyBuf = body;
        }
        totalLen += bodyBuf.length;
        if (header == null) {
            headerBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            headerBuf = header;
        }
        totalLen += headerBuf.length;
        if (uid == null) {
            uidBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            uidBuf = uid.getBytes();
        }
        totalLen += uidBuf.length;
        ByteBuf mate = Unpooled.buffer(32);
        mate.writeShort(RpcConsts.RPC_MAGIC);
        mate.writeInt(totalLen);
        mate.writeByte(RpcConsts.RPC_VERSION);
        mate.writeByte(rq);
        mate.writeLong(id);
        mate.writeInt(uidBuf.length);
        mate.writeInt(headerBuf.length);
        mate.writeInt(bodyBuf.length);
        return Unpooled.wrappedBuffer(
            mate.array(), uidBuf, headerBuf, bodyBuf);
    }

    public static Consumer<Object> buildConsumer(RpcManager rpcMgr, ActorService actor) {
        final Throwable timeout = new Exception("rpc time out");
        return (param) -> {
            if (!(param instanceof RpcCmd)) {return;}
            final RpcCmd temp = (RpcCmd)param;
            final long msgId = temp.getMsgId();
            final long userId = temp.getUserId();
            Object obj = rpcMgr.removePendMsg(userId, msgId);
            if (!(obj instanceof RpcCmd)) {return;}
            final RpcCmd cmd = (RpcCmd)param;
            cmd.setEx(timeout);
            if (actor.dispatchAsyncRsp(userId, cmd)) {return;}
            ((RpcCmdExt)cmd).recycle();
        };
    }
}


