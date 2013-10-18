package io.fathom.cloud.state;

import io.fathom.cloud.persist.ZookeeperEntityManager;
import io.fathom.cloud.persist.ZookeeperEntityTransaction;
import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

public class ZookeeperStateStore extends StateStore {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperStateStore.class);
    private static final int LONG_DATA_THRESHOLD = 8192;

    @Inject
    ZookeeperClient zk;

    @Inject
    Provider<ZookeeperEntityManager> entityManagerProvider;

    public class ZookeeperStateNode extends StateNode {
        final ZookeeperStateNode parent;
        final String key;

        int readVersion = -1;

        public ZookeeperStateNode(ZookeeperStateNode parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        @Override
        public List<String> getChildrenKeys() throws StateStoreException {
            try {
                String zkPath = getZkPath();
                log.debug("ZK getChildren on {}", zkPath);
                List<String> childKeys = zk.getChildren(zkPath, false);
                return childKeys;
            } catch (NoNodeException e) {
                return Lists.newArrayList();
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        @Override
        protected StateNode buildChild0(String key) {
            return new ZookeeperStateNode(this, key);
        }

        @Override
        public ByteString read(SettableFuture<Object> future) throws StateStoreException {
            try {
                String zkPath = getZkPath();
                log.debug("ZK read on {}", zkPath);

                Stat stat = new Stat();
                byte[] data = zk.getData(zkPath, future, stat);

                if (readVersion != -1) {
                    log.warn("Duplicate read on " + zkPath);
                    if (readVersion != stat.getVersion()) {
                        log.warn("Read version out of date on {} readVersion={}", zkPath, readVersion);
                        throw new OptimisticLockException();
                    }
                } else {
                    readVersion = stat.getVersion();
                }

                return ByteString.copyFrom(data);
            } catch (NoNodeException e) {
                return null;
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        @Override
        public List<StateNode> getChildren() throws StateStoreException {
            List<StateNode> nodes = Lists.newArrayList();
            for (String childKey : getChildrenKeys()) {
                nodes.add(child(childKey));
            }
            return nodes;
        }

        @Override
        public String getPath() {
            StringBuilder sb = new StringBuilder();
            getPath(sb);
            return sb.toString();
        }

        private void getPath(StringBuilder sb) {
            if (parent != null) {
                parent.getPath(sb);
            }

            sb.append("/");
            sb.append(key);
        }

        @Override
        public boolean create(ByteString data) throws StateStoreException {
            try {
                byte[] bytes = data.toByteArray();
                String zkPath = getZkPath();

                log.debug("ZK create on {}", zkPath);

                String createdZkPath = zk.create(zkPath, bytes, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                // TODO: Should we do a read so that our version tracking works
                // TODO: ZK should return a stat, surely??
                // TODO: Otherwise, we should return the value
                // read();

                return true;
            } catch (NoNodeException e) {
                mkdirs();
                return create(data);
            } catch (NodeExistsException e) {
                return false;
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        private void mkdirs() throws StateStoreException {
            try {
                zk.mkdirs(parent.getZkPath());
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        @Override
        public boolean delete() throws StateStoreException, OptimisticLockException {
            String zkPath = getZkPath();
            try {
                log.debug("ZK delete on {}", zkPath);

                if (readVersion == -1) {
                    // For now, this is a hard-error
                    log.error("Delete without read on zk path {}", zkPath);
                    throw new IllegalStateException("Delete without read on zkPath: " + zkPath);
                }

                zk.delete(zkPath, readVersion);
                return true;
            } catch (NoNodeException e) {
                return false;
            } catch (BadVersionException e) {
                log.warn("BadVersionException on delete:{} readVersion={}", zkPath, readVersion);
                throw new OptimisticLockException();
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        @Override
        public void update(ByteString data) throws StateStoreException, OptimisticLockException {
            byte[] bytes = data.toByteArray();
            String zkPath = getZkPath();
            if (bytes.length > LONG_DATA_THRESHOLD) {
                log.warn("Long data going into ZK: {} size={}", zkPath, bytes.length);
            }
            try {
                log.debug("ZK setData on {}", zkPath);

                if (readVersion == -1) {
                    // For now, this is a hard-error
                    throw new IllegalStateException("Attempt to update unread item: " + zkPath);
                }

                try {
                    Stat stat = zk.setData(zkPath, bytes, readVersion);
                } catch (NoNodeException e) {
                    create(data);
                }
            } catch (BadVersionException e) {
                log.warn("BadVersionException on update:{} readVersion={}", zkPath, readVersion);
                throw new OptimisticLockException();
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        @Override
        public boolean exists() throws StateStoreException {
            try {
                String zkPath = getZkPath();
                log.debug("ZK stat on {}", zkPath);

                Stat stat = zk.exists(zkPath, false);
                return stat != null;
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

        private String getZkPath() {
            return getPath();
        }

        @Override
        public Long getChildrenChangeCount() throws StateStoreException {
            try {
                String zkPath = getZkPath();
                log.debug("ZK stat on {}", zkPath);

                Stat stat = zk.exists(zkPath, false);
                if (stat != null) {
                    return Long.valueOf(stat.getCversion());
                }
                return null;
            } catch (KeeperException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            } catch (IOException e) {
                throw new StateStoreException("Error communicating with zookeeper", e);
            }
        }

    }

    @Override
    public ZookeeperStateNode getRoot(String id) {
        ZookeeperEntityManager entityManager = entityManagerProvider.get();
        ZookeeperEntityTransaction transaction = entityManager.getTransaction();
        ZookeeperStateNode stateNode = transaction.findRoot(id);
        if (stateNode == null) {
            stateNode = new ZookeeperStateNode(null, id);
            transaction.putRoot(id, stateNode);
        }
        return stateNode;
    }

    @Override
    public IdProvider getIdProvider(String name) {
        String path = "/ids/" + name;
        return new ZookeeperNodeVersionIdProvider(zk, path);
    }

}
