package com.vraft.core.rpc.transport;

import com.vraft.core.rpc.transport.NettyCodec.Decoder;
import com.vraft.core.rpc.transport.NettyCodec.Encoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/10 10:42
 **/
public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    private final static Logger logger = LogManager.getLogger(ClientInitializer.class);

    public ClientInitializer() {

    }

    @Override
    protected void initChannel(SocketChannel sc) throws Exception {
        final ChannelPipeline pipe = sc.pipeline();
        pipe.addLast(new Decoder());
        pipe.addLast(new Encoder());
        pipe.addLast(NettyCommon.newIdleHandler(120));

    }
}
