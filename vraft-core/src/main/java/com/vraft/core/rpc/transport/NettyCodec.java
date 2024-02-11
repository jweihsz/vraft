package com.vraft.core.rpc.transport;

import java.util.List;

import com.vraft.facade.rpc.RpcConsts;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * @author jweihsz
 * @version 2024/2/10 12:15
 **/
public class NettyCodec {

    public static class Encoder extends MessageToMessageEncoder<ByteBuf> {
        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf bf,
            List<Object> list) throws Exception {
            list.add(bf);
        }
    }

    public static class Decoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf bf,
            List<Object> list) throws Exception {
            while (bf.isReadable()) {
                Object res = process(ctx, bf);
                if (res == null) {break;}
                list.add(res);
            }
        }

        private Object process(ChannelHandlerContext ctx, ByteBuf bf) {
            final int ri = bf.readerIndex();
            final short magic = bf.readShort();
            if (magic != RpcConsts.RPC_MAGIC) {
                ctx.channel().close();
                return null;
            }
            final int totalLen = bf.readInt();
            if (bf.readableBytes() < totalLen) {
                bf.setIndex(ri, bf.writerIndex());
                return null;
            }
            return bf.readRetainedSlice(totalLen);
        }
    }
}
