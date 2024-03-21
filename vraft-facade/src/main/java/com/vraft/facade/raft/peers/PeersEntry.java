package com.vraft.facade.raft.peers;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/20 20:39
 **/
@Data
public class PeersEntry {
    private PeersCfg curConf;
    private PeersCfg oldConf;

    public static PeersEntry build() {
        PeersEntry res = new PeersEntry();
        res.setCurConf(new PeersCfg());
        res.setOldConf(new PeersCfg());
        return res;
    }
}
