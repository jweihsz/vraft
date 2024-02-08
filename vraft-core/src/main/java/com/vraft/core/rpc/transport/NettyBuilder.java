package com.vraft.core.rpc.transport;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;

/**
 * @author jweihsz
 * @version 2024/2/8 22:36
 **/
public class NettyBuilder {

    private int wire;
    private int type;
    private int bossNum;
    private int workerNum;
    private int port;
    private String host;
    private ChannelInitializer<?> initializer;
    private Map<ChannelOption<?>, Object> opts;
    private Map<ChannelOption<?>, Object> childOpts;

    public NettyBuilder() {
        this.opts = new HashMap<>();
        this.childOpts = new HashMap<>();
    }

    public int getPort() {
        return port;
    }

    public NettyBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return host;
    }

    public NettyBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public int getType() {
        return type;
    }

    public NettyBuilder setType(int type) {
        this.type = type;
        return this;
    }

    public int getWire() {
        return wire;
    }

    public NettyBuilder setWire(int wire) {
        this.wire = wire;
        return this;
    }

    public int getBossNum() {
        return bossNum;
    }

    public NettyBuilder setBossNum(int bossNum) {
        this.bossNum = bossNum;
        return this;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public NettyBuilder setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
        return this;
    }

    public ChannelInitializer<?> getInitializer() {
        return initializer;
    }

    public NettyBuilder setInitializer(ChannelInitializer<?> initializer) {
        this.initializer = initializer;
        return this;
    }

    public Map<ChannelOption<?>, Object> getOpts() {
        return opts;
    }

    public NettyBuilder setOpts(Map<ChannelOption<?>, Object> opts) {
        this.opts = opts;
        return this;
    }

    public Map<ChannelOption<?>, Object> getChildOpts() {
        return childOpts;
    }

    public NettyBuilder setChildOpts(Map<ChannelOption<?>, Object> childOpts) {
        this.childOpts = childOpts;
        return this;
    }

}
