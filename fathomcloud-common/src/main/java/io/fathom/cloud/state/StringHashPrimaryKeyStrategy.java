package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.state.StateStore.StateNode;

import java.nio.ByteBuffer;
import java.util.Random;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;

public class StringHashPrimaryKeyStrategy implements PrimaryKeyIndex {

    private final StateNode parentNode;
    private final FieldDescriptor idField;
    private final FieldDescriptor keyField;

    public StringHashPrimaryKeyStrategy(StateNode parentNode, FieldDescriptor idField, FieldDescriptor keyField) {
        this.parentNode = parentNode;
        this.idField = idField;
        this.keyField = keyField;
    }

    <T extends Message> String getKey(T item) {
        return (String) item.getField(keyField);
    }

    public <T extends GeneratedMessage> T find(NumberedItemCollection<T> collection, String key) throws CloudException {
        byte[] hash = Hashing.murmur3_128().hashString(key, Charsets.UTF_8).asBytes();
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        long v = buffer.getLong();

        T item = collection.find(v, StoreOptions.ShowDeleted);
        if (item != null) {
            if (key.equals(getKey(item))) {
                if (!ItemStates.isDeleted(item)) {
                    return item;
                }
            }
        }

        long seed = buffer.getLong();
        Random random = new Random(seed);

        while (true) {
            long delta = random.nextLong();
            long id = v ^ delta;

            item = collection.find(id);
            if (item != null) {
                if (key.equals(getKey(item))) {
                    if (!ItemStates.isDeleted(item)) {
                        return item;
                    }
                }
            }

            if (item == null) {
                return null;
            }
        }
    }

    @Override
    public <T extends GeneratedMessage> long createId(NumberedItemCollection<T> collection,
            GeneratedMessage.Builder item) throws DuplicateValueException, CloudException {
        String keyValue = (String) item.getField(keyField);
        if (keyValue == null) {
            throw new IllegalArgumentException("String key cannot be null: " + keyField);
        }

        byte[] hash = Hashing.murmur3_128().hashString(keyValue, Charsets.UTF_8).asBytes();
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        long v = buffer.getLong();

        T existing = collection.find(v, StoreOptions.ShowDeleted);
        if (existing == null) {
            return v;
        }

        if (keyValue.equals(getKey(existing))) {
            if (!ItemStates.isDeleted(existing)) {
                throw new DuplicateValueException();
            }
        }

        long seed = buffer.getLong();
        Random random = new Random(seed);

        while (true) {
            long delta = random.nextLong();
            long id = v ^ delta;

            existing = collection.find(id, StoreOptions.ShowDeleted);
            if (existing == null) {
                return id;
            }
            if (keyValue.equals(getKey(existing))) {
                if (!ItemStates.isDeleted(existing)) {
                    throw new DuplicateValueException();
                }
            }
        }
    }

    // private boolean hasChild(long id) throws StateStoreException {
    // return parentNode.hasChild(Long.toHexString(id));
    // }

    private <T extends GeneratedMessage> String findItemKey(NumberedItemCollection<T> collection, long id)
            throws CloudException {
        T item = collection.find(id);
        if (item == null) {
            return null;
        }
        String key = getKey(item);
        if (key == null) {
            throw new IllegalStateException();
        }
        return key;
    }

    @Override
    public boolean allowDelete() {
        // We can't allow delete (we have to use tombstones instead)
        // Otherwise we wouldn't know when to stop the hash walk
        return false;
    }

}
