package com.vraft.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bootstrap {
    private final static Logger logger = LogManager.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Exception {
        logger.info("hello world!");
    }
}
