package com.imooc.curator.utils;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ChangLiang
 * @date 2019/3/12
 */
public class ZKCurator {

    // zk客户端
    private CuratorFramework client = null;

    final static Logger Log = LoggerFactory.getLogger(ZKCurator.class);

    public ZKCurator(CuratorFramework client) {
        this.client = client;
    }

    public void init() {
        client = client.usingNamespace("zk-curator-connector");
    }

    public String isZKAlive() {
        return client.getState().name();
    }


}
