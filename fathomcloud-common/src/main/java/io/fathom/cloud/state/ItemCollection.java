package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.state.StateStore.StateNode;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

public class ItemCollection {
    final StateNode parentNode;
    final Codec codec;

    public ItemCollection(StateNode parentNode, Codec codec) {
        this.parentNode = parentNode;
        if (codec == null) {
            codec = new ProtobufCodec();
        }
        this.codec = codec;
    }

    public ItemCollection(StateNode parentNode) {
        this(parentNode, null);
    }

    protected Message update(StateNode node, Message.Builder item) throws CloudException {
        Message msg = toMessage(item);

        ByteString data;
        try {
            data = codec.serialize(msg);
        } catch (IOException e) {
            throw new CloudException("Error serializing data", e);
        }

        node.update(data);
        return msg;
    }

    protected Message toMessage(MessageOrBuilder item) {
        Message msg;
        if (item instanceof Message) {
            msg = (Message) item;
        } else {
            msg = ((GeneratedMessage.Builder) item).build();
        }
        return msg;
    }

    protected <T> List<T> deserializeChildren(StateNode parent, Builder builder) throws CloudException {
        List<T> items = Lists.newArrayList();
        for (StateNode child : parent.getChildren()) {
            builder.clear();
            T item = (T) deserialize(child, builder);
            items.add(item);
        }
        return items;
    }

    <T extends Builder, V extends Message> V deserialize(StateNode node, Builder<T> builder) throws StateStoreException {
        return deserialize(node, builder, null);
    }

    <T extends Builder, V extends Message> V deserialize(StateNode node, Builder<T> builder,
            SettableFuture<Object> watch) throws StateStoreException {
        ByteString data = node.read(watch);
        if (data == null) {
            return null;
        }
        try {
            V v = (V) codec.deserialize(builder, data);
            return v;
        } catch (IOException e) {
            throw new StateStoreException("Error reading item: " + node.getPath(), e);
        }
    }
}
