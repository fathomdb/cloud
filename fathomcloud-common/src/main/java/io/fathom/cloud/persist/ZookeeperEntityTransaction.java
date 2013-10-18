package io.fathom.cloud.persist;

import io.fathom.cloud.state.ZookeeperStateStore.ZookeeperStateNode;

import java.util.Map;

import javax.persistence.EntityTransaction;

import com.google.common.collect.Maps;

public class ZookeeperEntityTransaction implements EntityTransaction {

    private boolean active;
    private boolean rollbackOnly;

    @Override
    public void begin() {
        if (active) {
            throw new IllegalStateException();
        }

        this.rollbackOnly = false;
        this.roots.clear();
        this.active = true;
    }

    @Override
    public void commit() {
        checkActive();
        if (rollbackOnly) {
            throw new IllegalStateException();
        }
        active = false;
        this.roots.clear();
    }

    @Override
    public void rollback() {
        checkActive();
        active = false;
        this.roots.clear();
    }

    @Override
    public void setRollbackOnly() {
        checkActive();
        rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        checkActive();
        return rollbackOnly;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    final Map<String, ZookeeperStateNode> roots = Maps.newHashMap();

    public void putRoot(String id, ZookeeperStateNode stateNode) {
        checkActive();

        roots.put(id, stateNode);
    }

    public ZookeeperStateNode findRoot(String id) {
        checkActive();
        return roots.get(id);
    }

    private void checkActive() {
        if (!active) {
            throw new IllegalStateException();
        }
    }
}
