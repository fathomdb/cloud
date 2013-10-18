package io.fathom.cloud.state;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

public abstract class StateStore {
    public static abstract class StateNode {
        protected final Map<String, StateNode> children = Maps.newHashMap();

        public abstract List<String> getChildrenKeys() throws StateStoreException;

        public final StateNode child(String key) {
            StateNode child = children.get(key);
            if (child == null) {
                child = buildChild0(key);
                children.put(key, child);
            }
            return child;
        }

        protected abstract StateNode buildChild0(String key);

        public boolean hasChild(String key) throws StateStoreException {
            StateNode child = child(key);
            return child.exists();
        }

        public abstract boolean exists() throws StateStoreException;

        public abstract ByteString read(SettableFuture<Object> watch) throws StateStoreException;

        public abstract List<StateNode> getChildren() throws StateStoreException;

        public abstract String getPath();

        public abstract boolean create(ByteString data) throws StateStoreException;

        public abstract void update(ByteString data) throws StateStoreException;

        public abstract boolean delete() throws StateStoreException;

        public abstract Long getChildrenChangeCount() throws StateStoreException;
    }

    public abstract StateNode getRoot(String id);

    public abstract IdProvider getIdProvider(String name);
}
