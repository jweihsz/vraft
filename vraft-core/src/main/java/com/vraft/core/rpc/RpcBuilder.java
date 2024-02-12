package com.vraft.core.rpc;

/**
 * @author jweihsz
 * @version 2024/2/8 22:36
 **/
public class RpcBuilder {
    private int wire;
    private int type;
    private int bossNum;
    private int workerNum;
    private int port;
    private String host;

    public RpcBuilder() {}

    public int getPort() {
        return port;
    }

    public RpcBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return host;
    }

    public RpcBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public int getType() {
        return type;
    }

    public RpcBuilder setType(int type) {
        this.type = type;
        return this;
    }

    public int getWire() {
        return wire;
    }

    public RpcBuilder setWire(int wire) {
        this.wire = wire;
        return this;
    }

    public int getBossNum() {
        return bossNum;
    }

    public RpcBuilder setBossNum(int bossNum) {
        this.bossNum = bossNum;
        return this;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public RpcBuilder setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
        return this;
    }
}
