package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/6 16:47
 **/
public interface BrokerServer {

    String getName();

    void startup();

    void shutdown();
}
