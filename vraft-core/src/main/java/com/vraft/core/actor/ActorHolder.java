package com.vraft.core.actor;

import com.vraft.facade.actor.ActorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/8 03:26
 **/
public class ActorHolder implements ActorService {
    private final static Logger logger = LogManager.getLogger(ActorHolder.class);
}
