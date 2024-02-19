package com.vraft.core.rpc;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
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

    /* version(byte)|type(byte)|seq(long)|uid-size(int)|
     * header-size(int)|body-size(int)|uid-content(bytes)|
     * header-content(bytes)|body-content(bytes)
     */
    public static int RPC_MATE_SIZE = 1 + 1 + 8 + 4 + 4 + 4;

    public static final byte[] EMPTY_BUFFER = new byte[0];

    public static final AttributeKey<Long> CH_KEY;
    public static final AttributeKey<Long> CH_ACTOR;
    public static final AttributeKey<Map<Long, Object>> CH_PEND;

    static {
        CH_KEY = AttributeKey.valueOf("ch_key");
        CH_ACTOR = AttributeKey.valueOf("ch_actor");
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

    public static long getUserActor(Channel ch) {
        if (ch == null) {return -1L;}
        return ch.attr(RpcCommon.CH_ACTOR).get();
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
}


