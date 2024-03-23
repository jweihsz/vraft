package com.vraft.core.rpc;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.core.utils.SystemUtil;
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
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/6 16:51
 **/
public class RpcCommon {
    private final static Logger logger = LogManager.getLogger(RpcCommon.class);

    private RpcCommon() {}

    //client connect time out
    public static int CONN_TIMEOUT = 3000;

    public static int RPC_MATE_SIZE = 1/*version(byte)*/
        + 1/*biz(byte)*/ + 1/*type(byte)*/
        + 8/*seq(long)*/ + 4/*uid-size(int)*/
        + 4/*header-size(int)*/ + 4/*body-size(int)*/;
    public static final byte[] EMPTY_BUFFER = new byte[0];

    public static final AttributeKey<Long> CH_KEY;
    public static final AttributeKey<String> HOST_KEY;
    public static final AttributeKey<Map<Long, Object>> CH_PEND;

    public static final EventLoopGroup BOSS_GROUP;
    public static final EventLoopGroup WORKER_GROUP;

    static {
        CH_KEY = AttributeKey.valueOf("ch_key");
        HOST_KEY = AttributeKey.valueOf("host_key");
        CH_PEND = AttributeKey.valueOf("ch_resp_pend");
        BOSS_GROUP = eventLoop(1);
        WORKER_GROUP = eventLoop(SystemUtil.getPhyCpuNum());
    }

    public static void shutdownBoss() {
        BOSS_GROUP.shutdownGracefully()
            .syncUninterruptibly();
    }

    public static void shutdownWorker() {
        WORKER_GROUP.shutdownGracefully()
            .syncUninterruptibly();
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
        if (arr.length != 2) {throw new RuntimeException();}
        return new InetSocketAddress(arr[0], Integer.parseInt(arr[1]));
    }

    public static InetSocketAddress localAddress(Channel ch) {
        return (InetSocketAddress)ch.localAddress();
    }

    public static InetSocketAddress remoteAddress(Channel ch) {
        return (InetSocketAddress)ch.remoteAddress();
    }

    public static String remoteAddressStr(Channel ch) {
        InetSocketAddress sad = RpcCommon.remoteAddress(ch);
        return sad.getHostString() + ":" + sad.getPort();
    }

    public static IdleStateHandler newIdleHandler(int idle) {
        return new IdleStateHandler(0, 0, idle);
    }

    public static ByteBuf convert(String str) {
        return Unpooled.wrappedBuffer(str.getBytes());
    }

    public static boolean checkRpcMate(ByteBuf bf) {
        int total = bf.readableBytes();
        return total >= RPC_MATE_SIZE;
    }

    public static int getRpcUidSize(ByteBuf bf) {
        int index = bf.readerIndex() + 11;
        return bf.getInt(index);
    }

    public static int getRpcUidIndex(ByteBuf bf) {
        return bf.readerIndex() + RPC_MATE_SIZE;
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
        final ActorService actor = ctx.getActorSvs();
        final RpcCmd cmd = buildBaseCmd(userId, biz, type,
            msgId, uid, body, header, cb, timeout);
        if (actor.dispatchWriteChMsg(userId, cmd)) {return true;}
        ((RpcCmdExt)cmd).recycle();
        return false;
    }

    public static boolean dispatchOneWay(SystemCtx ctx, long userId,
        byte biz, String uid, byte[] header, byte[] body) throws Exception {
        final UidService genId = ctx.getUidSvs();
        return dispatchRpc(ctx, userId, biz, RpcConsts.RPC_ONE_WAY,
            genId.genMsgId(), uid, header, body, -1L, null);
    }

    public static boolean dispatchTwoWay(SystemCtx ctx, long userId,
        byte biz, String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception {
        final UidService genId = ctx.getUidSvs();
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
        final RpcManager mgr = ctx.getRpcMgr();
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
            ch.writeAndFlush(cbf.retain());
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
        final RpcManager mgr = ctx.getRpcMgr();
        TimerService timer = ctx.getTimerSvs();
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
        if (uid == null) {
            uidBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            uidBuf = uid.getBytes();
        }
        totalLen += uidBuf.length;
        if (header == null) {
            headerBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            headerBuf = header;
        }
        totalLen += headerBuf.length;

        if (body == null) {
            bodyBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            bodyBuf = body;
        }
        totalLen += bodyBuf.length;

        byte[] mateArr = new byte[RpcCommon.RPC_MATE_SIZE + 6];
        ByteBuf mate = Unpooled.wrappedBuffer(mateArr);
        mate.resetWriterIndex();
        mate.writeShort(RpcConsts.RPC_MAGIC);
        mate.writeInt(totalLen);
        mate.writeByte(RpcConsts.RPC_VERSION);
        mate.writeByte(rq);
        mate.writeByte(0x00);
        mate.writeLong(id);
        mate.writeInt(uidBuf.length);
        mate.writeInt(headerBuf.length);
        mate.writeInt(bodyBuf.length);
        return Unpooled.wrappedBuffer(mateArr, uidBuf, headerBuf, bodyBuf);
    }
}


