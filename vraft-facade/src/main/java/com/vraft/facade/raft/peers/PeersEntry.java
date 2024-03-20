package com.vraft.facade.raft.peers;

import lombok.Data;

/**
 * @author jweihsz
 * @version 2024/3/20 20:39
 **/
@Data
public class PeersCfgEntry {
    private PeersCfgNode conf = new PeersCfgNode();
    private PeersCfgNode oldConf = new PeersCfgNode();
}
