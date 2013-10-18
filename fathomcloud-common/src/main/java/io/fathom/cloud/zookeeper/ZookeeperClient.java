package io.fathom.cloud.zookeeper;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.util.concurrent.SettableFuture;

public class ZookeeperClient {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperClient.class);

    ZooKeeper zk;

    private final String connectString;

    public ZookeeperClient(String connectString) {
        log.info("Building zookeeper client to {}", connectString);
        this.connectString = connectString;
    }

    protected synchronized ZooKeeper getZk() throws IOException {
        ZooKeeper zk = this.zk;
        if (zk == null) {
            int sessionTimeout = 20000;
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    log.debug("Got ZK event {}", event);
                }
            };
            try {
                zk = new ZooKeeper(connectString, sessionTimeout, watcher);
            } catch (IOException e) {
                throw new IOException("Unable to connect to zookeeper", e);
            }
            this.zk = zk;
        }
        return zk;
    }

    protected synchronized ZooKeeper closeZk() {
        ZooKeeper zk = this.zk;
        if (zk != null) {
            this.zk = null;

            try {
                zk.close();
            } catch (Exception e) {
                log.warn("Error closing zk", e);
            }
        }
        return zk;
    }

    public Stat exists(String path, boolean watch) throws KeeperException, IOException {
        try {
            return getZk().exists(path, watch);
        } catch (KeeperException e) {
            if (processException(e)) {
                return exists(path, watch);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    public void delete(String path, int version) throws KeeperException, IOException {
        try {
            getZk().delete(path, version);
        } catch (KeeperException e) {
            if (processException(e)) {
                delete(path, version);
                return;
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    public List<String> getChildren(String path, boolean watch) throws KeeperException, IOException {
        try {
            return getZk().getChildren(path, watch);
        } catch (KeeperException e) {
            if (processException(e)) {
                return getChildren(path, watch);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    public byte[] getData(String path, SettableFuture<Object> watch, Stat stat) throws IOException, KeeperException {
        try {
            Watcher watcher = null;

            if (watch != null) {
                watcher = new ListenableWatcher(watch);
            }

            return getZk().getData(path, watcher, stat);
        } catch (KeeperException e) {
            if (processException(e)) {
                return getData(path, watch, stat);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    public Stat setData(String path, byte[] data, int version) throws KeeperException, IOException {
        try {
            return getZk().setData(path, data, version);
        } catch (KeeperException e) {
            if (processException(e)) {
                return setData(path, data, version);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    private boolean processException(KeeperException e) {
        Code code = e.code();
        switch (code) {
        case SESSIONEXPIRED:
            log.warn("Closing ZK session after SESSIONEXPIRED");
            closeZk();
            return true;
        case CONNECTIONLOSS:
            log.warn("Closing ZK session after CONNECTIONLOSS");
            closeZk();
            return true;

        default:
            return false;
        }
    }

    public String create(final String path, byte[] data, List<ACL> acl, CreateMode createMode) throws IOException,
            KeeperException {
        try {
            return getZk().create(path, data, acl, createMode);
        } catch (KeeperException e) {
            if (processException(e)) {
                return create(path, data, acl, createMode);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with zookeeper", e);
        }
    }

    public void mkdirs(String path) throws KeeperException, IOException {
        try {
            log.debug("ZK create on {}", path);

            getZk().create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (NoNodeException e) {
            mkdirs(getParent(path));
            mkdirs(path);
        } catch (NodeExistsException e) {
            return;
        } catch (KeeperException e) {
            if (processException(e)) {
                mkdirs(path);
                return;
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Error communicating with zookeeper", e);
        }
    }

    public static String getParent(String zkPath) {
        zkPath = CharMatcher.is('/').trimTrailingFrom(zkPath);

        int lastSlash = zkPath.lastIndexOf('/');
        if (lastSlash == -1) {
            throw new IllegalArgumentException();
        }
        return zkPath.substring(0, lastSlash);
    }

    public void createOrUpdate(String path, byte[] data, boolean mkdirs) throws KeeperException, IOException {
        try {
            setData(path, data, -1);
        } catch (NoNodeException e) {
            create(path, data, mkdirs);
        }
    }

    public String create(final String path, byte[] data, boolean mkdirs) throws IOException, KeeperException {
        try {
            return create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (NoNodeException e) {
            if (mkdirs) {
                mkdirs(getParent(path));
                return create(path, data, false);
            } else {
                throw e;
            }
        }
    }

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);

            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            } else {
                switch (c) {
                case '.':
                case ':':
                case '-':
                    sb.append(c);
                    break;

                default: {
                    String v = Integer.toHexString(c);
                    switch (v.length()) {
                    case 1:
                        sb.append("_0" + v);
                        break;

                    case 2:
                        sb.append("_" + v);
                        break;

                    case 3:
                        sb.append("__0" + v);
                        break;

                    case 4:
                        sb.append("__" + v);
                        break;

                    default:
                        throw new UnsupportedOperationException();
                    }
                }
                    break;
                }
            }
        }

        return sb.toString();
    }

}
