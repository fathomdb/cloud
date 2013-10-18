package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.state.StateStore.StateNode;
import io.fathom.cloud.zookeeper.ZookeeperClient;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

public class NamedItemCollection<T extends GeneratedMessage> extends ItemCollection {
    final GeneratedMessage.Builder template;
    final FieldDescriptor nameField;
    final Descriptor descriptor;

    public NamedItemCollection(StateNode parentNode, GeneratedMessage.Builder template, FieldDescriptor nameField) {
        super(parentNode);
        this.template = template;
        this.nameField = nameField;

        this.descriptor = template.getDescriptorForType();
    }

    private boolean usesItemState() {
        return ItemStates.usesItemState(descriptor);
    }

    public List<T> list() throws CloudException {
        List<T> items = deserializeChildren(parentNode, template.clone());
        if (!usesItemState()) {
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

    public void delete(String name) throws StateStoreException {
        StateNode node = getNode(name);

        node.delete();
    }

    public T find(String name) throws CloudException {
        StateNode node = getNode(name);

        T t = (T) deserialize(node, template.clone());
        if (t != null && usesItemState() && ItemStates.isDeleted(t)) {
            return null;
        }
        return t;
    }

    public Watched<T> watch(String name) throws CloudException {
        StateNode node = getNode(name);

        SettableFuture<Object> future = SettableFuture.create();

        T t = (T) deserialize(node, template.clone(), future);
        if (t != null && usesItemState() && ItemStates.isDeleted(t)) {
            return null;
        }

        return new Watched(t, future);
    }

    private StateNode getNode(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException();
        }

        StateNode node = parentNode.child(ZookeeperClient.escape(name));
        return node;
    }

    public T create(Message.Builder item) throws CloudException, DuplicateValueException {
        if (usesItemState()) {
            ItemStates.setCreatedAt(item);
        }

        String name = getKey(item);

        StateNode node = getNode(name);

        T built = (T) toMessage(item);

        ByteString data = built.toByteString();

        if (!node.create(data)) {
            throw new DuplicateValueException();
        }

        return built;
    }

    public T update(Message.Builder item) throws CloudException {
        if (usesItemState()) {
            ItemStates.setUpdatedAt(item);
        }

        String name = getKey(item);
        StateNode node = getNode(name);

        return (T) update(node, item);
    }

    private String getKey(MessageOrBuilder item) {
        String name = (String) item.getField(nameField);
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException();
        }
        return name;
    }

    public String getEtag() throws StateStoreException {
        Long v = parentNode.getChildrenChangeCount();
        if (v == null) {
            return "-";
        }
        return v.toString();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public NamedItemCollection<T> create() {
            FieldDescriptor idField = getIdField(template);
            if (idField.getType() != Type.STRING) {
                throw new IllegalArgumentException();
            }

            if (keyFieldNumber != null) {
                throw new IllegalArgumentException();
            }

            return new NamedItemCollection(parentNode, template, idField);
        }
    }

    public static <T extends GeneratedMessage> CollectionBuilder<T> builder(StateNode parentNode, Class<T> protobufClass) {
        return new CollectionBuilder<T>(parentNode, protobufClass);
    }

}
