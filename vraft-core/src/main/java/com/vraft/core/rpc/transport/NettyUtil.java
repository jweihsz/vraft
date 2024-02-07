package com.vraft.core.rpc.transport;

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

/**
 * @author jweihsz
 * @version 2024/2/6 16:51
 **/
public class NettyUtil {
    private NettyUtil() {}

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

    public static EventLoopGroup newEventLoopGroup(int nThreads) {
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

}
