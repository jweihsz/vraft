package com.vraft.core.rpc;

import java.util.HashMap;
import java.util.Map;

import com.vraft.core.rpc.transport.NettyBuilder;
import com.vraft.core.rpc.transport.NettyClient;
import com.vraft.core.rpc.transport.NettyCommon;
import com.vraft.core.rpc.transport.NettyServer;
import com.vraft.core.rpc.transport.ServerInitializer;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.rpc.RpcService;
import com.vraft.facade.system.SystemCtx;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweih.hjw
 * @version 2024/2/5 14:53
 */
public class RpcHolder implements RpcService {
    private final static Logger logger = LogManager.getLogger(RpcHolder.class);

    private final SystemCtx sysCtx;
    
    public RpcHolder(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
    }

    @Override
    public void startup() throws Exception {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerProcessor(String uid, RpcProcessor<?> processor) {
        if (uid == null || uid.isEmpty() || processor == null) {return;}
        NettyCommon.PROCESSOR.put(uid, processor);
    }

    @Override
    public void unregisterProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        NettyCommon.PROCESSOR.remove(uid);
    }

    @Override
    public RpcProcessor<?> getProcessor(String uid) {
        return NettyCommon.PROCESSOR.get(uid);
    }

    private NettyServer raftServerInstance() {
        NettyBuilder b = raftServerBuilder();
        return new NettyServer(b);
    }

    private NettyClient raftClientInstance() {
        NettyBuilder b = raftClientBuilder();
        return new NettyClient(b);
    }

    private NettyBuilder raftServerBuilder() {
        Map<ChannelOption<?>, Object> opts = new HashMap<>();
        opts.put(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        Map<ChannelOption<?>, Object> childOpts = new HashMap<>();
        childOpts.put(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        childOpts.put(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        childOpts.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        childOpts.put(ChannelOption.SO_RCVBUF, 1024 * 1024);
        childOpts.put(ChannelOption.SO_SNDBUF, 1024 * 1024);
        NettyBuilder b = new NettyBuilder();
        b.setBossNum(1).setWorkerNum(8);
        b.setInitializer(new ServerInitializer());
        b.setType(RpcConsts.SERVER).setWire(RpcConsts.TCP);
        b.setOpts(opts).setChildOpts(childOpts);
        return b;
    }

    private NettyBuilder raftClientBuilder() {
        Map<ChannelOption<?>, Object> opts = new HashMap<>();
        opts.put(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        NettyBuilder b = new NettyBuilder();
        b.setBossNum(4);
        b.setInitializer(new ServerInitializer());
        b.setType(RpcConsts.CLIENT).setWire(RpcConsts.TCP);
        b.setOpts(opts);
        return b;
    }

}
