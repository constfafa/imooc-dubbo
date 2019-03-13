package com.imooc.curator.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 分布式锁的实现工具类
 * @author ChangLiang
 * @date 2019/3/12
 */
public class DistriutedLock {

    private CuratorFramework client = null;

    private static Logger LOGGER = LoggerFactory.getLogger(DistriutedLock.class);

    // 用于挂起当前请求，而且等待上一个分布式锁释放
    private static CountDownLatch zkLockLatch = new CountDownLatch(1);

    // 分布式锁的总节点名
    private static final String ZK_LOCK_PROJECT = "imooc-locks";

    // 分布式锁节点
    private static final String DISTRIBUTED_LOCK = "distributed_lock";

    public DistriutedLock(CuratorFramework client) {
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
            // 针对zk的分布式锁节点 创建相应的watch事件监听
            addWatcherToLock("/" + ZK_LOCK_PROJECT);
        } catch (Exception e) {
            LOGGER.info("客户端连接zk server错误...");
        }

    }

    public void getLock() {

        // 使用死循环 并且仅当上一个锁释放并且当前请求获得锁成功后才会跳出
        while (true) {
            try {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath("/" + ZK_LOCK_PROJECT + "/" + DISTRIBUTED_LOCK);
                LOGGER.info("获得分布式锁成功...");
                // 如果锁的节点能够创建成功 则锁没有被占用
                return;
            } catch (Exception e) {
                LOGGER.info("获得分布式锁失败...");
                try {
                    if (zkLockLatch.getCount()<=0) {
                        zkLockLatch = new CountDownLatch(1);
                    }
                    // 阻塞线程
                    zkLockLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public boolean releaseLock() {
        try {
            if (client.checkExists().forPath("/"+ZK_LOCK_PROJECT+"/"+DISTRIBUTED_LOCK)!=null) {
                client.delete().forPath("/" + ZK_LOCK_PROJECT + "/" + DISTRIBUTED_LOCK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        LOGGER.info("分布式锁释放完毕");
        return true;
    }

    /**
     * 针对父节点的子节点 创建remove watch监听
     * @param path
     */
    public void addWatcherToLock(String path) {
        final PathChildrenCache cache = new PathChildrenCache(client, path, true);
        try {
            cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            cache.getListenable().addListener((client, event) -> {
                if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                    String tmpPath = event.getData().getPath();
                    LOGGER.info("上一个会话已释放锁或该会话已断开，节点路径为：" + tmpPath);
                    if (tmpPath.contains(DISTRIBUTED_LOCK)) {
                        LOGGER.info("释放计数器，让当前请求来获得分布式锁...");
                        zkLockLatch.countDown();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
