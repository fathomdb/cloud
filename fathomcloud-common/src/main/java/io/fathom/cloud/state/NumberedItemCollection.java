package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.state.StateStore.StateNode;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;

public class NumberedItemCollection<T extends GeneratedMessage> extends ItemCollection {

    final Builder<?> template;
    final FieldDescriptor idField;

    final PrimaryKeyIndex primaryKeyIndex;
    final Descriptor descriptor;

    public NumberedItemCollection(StateNode parentNode, Builder<?> template, FieldDescriptor idField) {
        this(parentNode, null, template, idField, null);
    }

    public NumberedItemCollection(StateNode parentNode, Codec codec, Builder<?> template, FieldDescriptor idField,
            FieldDescriptor keyField) {
        super(parentNode, codec);
        this.template = template;
        this.idField = idField;

        if (keyField == null) {
            this.primaryKeyIndex = new RandomPrimaryKeyStrategy(parentNode, idField);
        } else if (keyField.getType() == Type.STRING) {
            this.primaryKeyIndex = new StringHashPrimaryKeyStrategy(parentNode, idField, keyField);
        } else {
            throw new IllegalArgumentException("Unknown key field type: " + keyField);
        }

        this.descriptor = template.getDescriptorForType();
    }

    public List<T> list(StoreOptions... options) throws CloudException {
        List<T> items = deserializeChildren(parentNode, template.clone());
        if (!usesItemState() || showDeleted(options)) {
            return items;
        }

        List<T> ret = Lists.newArrayList();
        for (T item : items) {
            if (!ItemStates.isDeleted(item)) {
                ret.add(item);
            }
        }
        return ret;
    }

    public T find(long itemId, StoreOptions... options) throws CloudException {
        StateNode itemNode = parentNode.child(Long.toHexString(itemId));

        T t = (T) deserialize(itemNode, template.clone());
        if (t != null && usesItemState() && !showDeleted(options) && ItemStates.isDeleted(t)) {
            return null;
        }
        return t;
    }

    private static boolean showDeleted(StoreOptions[] options) {
        if (options == null || options.length == 0) {
            return false;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i] == StoreOptions.ShowDeleted) {
                return true;
            }
        }
        return false;
    }

    public T findByKey(String key) throws CloudException {
        StringHashPrimaryKeyStrategy pk = (StringHashPrimaryKeyStrategy) this.primaryKeyIndex;
        T t = pk.find(this, key);
        if (t != null && usesItemState() && ItemStates.isDeleted(t)) {
            return null;
        }
        return t;
    }

    public Watched<T> watch(long itemId) throws CloudException {
        StateNode itemNode = parentNode.child(Long.toHexString(itemId));

        SettableFuture<Object> future = SettableFuture.create();

        T t = (T) deserialize(itemNode, template.clone(), future);
        if (t != null && usesItemState() && ItemStates.isDeleted(t)) {
            return null;
        }

        return new Watched(t, future);
    }

    public T create(GeneratedMessage.Builder item) throws CloudException, DuplicateValueException {
        long id = ((Number) item.getField(idField)).longValue();

        if (usesItemState()) {
            ItemStates.setCreatedAt(item);
        }

        if (id != 0) {
            throw new IllegalStateException();
        }

        while (true) {
            id = primaryKeyIndex.createId(this, item);

            item.setField(idField, id);

            Message built = item.build();

            ByteString data;
            try {
                data = codec.serialize(built);
            } catch (IOException e) {
                throw new CloudException("Error serializing data", e);
            }
            StateNode node = parentNode.child(Long.toHexString(id));

            if (!node.create(data)) {
                continue;
            }

            return (T) built;
        }
    }

    // public T put(GeneratedMessage.Builder item) throws CloudException {
    // return (T) putItem(parentNode, item, idField);
    // }
    //
    // protected Message putItem(StateNode parent, GeneratedMessage.Builder
    // builder, FieldDescriptor idField)
    // throws CloudException {
    // long id = ((Number) builder.getField(idField)).longValue();
    // boolean isCreate = false;
    //
    // if (id == 0) {
    // isCreate = true;
    // }
    //
    // while (true) {
    // if (isCreate) {
    // // Assign a new user id, randomly
    // id = getRandom(Integer.MAX_VALUE);
    // if (parent.hasChild(Long.toHexString(id))) {
    // continue;
    // }
    // builder.setField(idField, id);
    // }
    //
    // Message built = builder.build();
    //
    // ByteString data;
    // try {
    // data = codec.serialize(built);
    // } catch (IOException e) {
    // throw new CloudException("Error serializing data", e);
    // }
    //
    // StateNode node = parent.child(Long.toHexString(id));
    //
    // if (isCreate) {
    // if (!node.create(data)) {
    // continue;
    // }
    // } else {
    // node.update(data);
    // }
    // return built;
    // }
    // }

    public T delete(long itemId) throws CloudException {
        if (usesItemState()) {
            T found = find(itemId);
            if (found == null) {
                return null;
            }
            ItemStates.markDeleted(this, found);
            return found;
        }

        if (!this.primaryKeyIndex.allowDelete()) {
            throw new UnsupportedOperationException();
        }
        StateNode itemNode = parentNode.child(Long.toHexString(itemId));

        T v = (T) deserialize(itemNode, template.clone());
        if (v == null) {
            return null;
        }

        if (!itemNode.delete()) {
            throw new IllegalStateException();
        }

        return v;
    }

    private boolean usesItemState() {
        return ItemStates.usesItemState(descriptor);
    }

    public T update(Message.Builder item) throws CloudException {
        return (T) update(parentNode, item, idField);
    }

    protected Message update(StateNode parent, Message.Builder item, FieldDescriptor idField) throws CloudException {
        if (usesItemState()) {
            ItemStates.setUpdatedAt(item);
        }

        long id = ((Number) item.getField(idField)).longValue();
        if (id == 0) {
            throw new IllegalArgumentException();
        }

        StateNode node = parent.child(Long.toHexString(id));

        return update(node, item);
    }

    public static class CollectionBuilder<T extends GeneratedMessage> extends CollectionBuilderBase<T> {
        public CollectionBuilder(StateNode parentNode, Class<T> protobufClass) {
            super(parentNode, protobufClass);
        }

        @Override
        public CollectionBuilder<T> idField(int idFieldNumber) {
            return (CollectionBuilder<T>) super.idField(idFieldNumber);
        }

        @Override
        public CollectionBuilder<T> keyField(int keyFieldNumber) {
            return (CollectionBuilder<T>) super.keyField(keyFieldNumber);
        }

        @Override
        public NumberedItemCollection<T> create() {
            Codec codec = null;

            FieldDescriptor idField = getIdField(template);

            FieldDescriptor keyField = getKeyField();

            return new NumberedItemCollection(parentNode, codec, template, idField, keyField);
        }
    }

    public static <T extends GeneratedMessage> CollectionBuilder<T> builder(StateNode parentNode, Class<T> protobufClass) {
        return new CollectionBuilder<T>(parentNode, protobufClass);
    }

}
