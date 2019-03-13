package com.imooc.curator.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分布式锁的实现工具类
 *
 * @author ChangLiang
 * @date 2019/3/12
 */
public class InnerDistriutedLock {

    private CuratorFramework client = null;

    private static Logger LOGGER = LoggerFactory.getLogger(InnerDistriutedLock.class);

    // 分布式锁的总节点名
    private static final String ZK_LOCK_PROJECT = "imooc-locks";

    // 分布式锁节点
    private static final String DISTRIBUTED_LOCK = "inner_distributed_lock";

    private InterProcessMutex lock = null;

    public InnerDistriutedLock(CuratorFramework client) {
        this.client = client;
    }

    public void init() {
        client = client.usingNamespace("ZKLock-Namespace");
        /**
         * 创建zk锁的总节点，相当于eclipse的工作空间下的项目
         * ZKLock-Namespace
         *   |
         *   ——imooc-locks
         *     |
         *     ——distributed_lock
         */
        try {
            if (client.checkExists().forPath("/" + ZK_LOCK_PROJECT) == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath("/" + ZK_LOCK_PROJECT);
            }
            lock = new InterProcessMutex(client, "/" + ZK_LOCK_PROJECT + "/" + DISTRIBUTED_LOCK);
        } catch (Exception e) {
            LOGGER.info("客户端连接zk server错误...");
        }

    }

    public void getLock() {
        if (lock != null) {
            try {
                lock.acquire();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void releaseLock() {
        try {
            lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
