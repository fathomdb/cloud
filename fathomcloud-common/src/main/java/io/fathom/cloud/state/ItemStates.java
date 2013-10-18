package io.fathom.cloud.state;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.ProtobufUtils;
import io.fathom.cloud.protobuf.CloudCommons.ItemStateData;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class ItemStates {

    static class ClassState {
        final Descriptor descriptor;

        Optional<FieldDescriptor> itemStateField;

        public ClassState(Descriptor descriptor) {
            this.descriptor = descriptor;
        }

        public synchronized Optional<FieldDescriptor> getItemStateField() {
            if (itemStateField == null) {
                FieldDescriptor found = null;
                for (FieldDescriptor field : descriptor.getFields()) {
                    if (field.getType() != Type.MESSAGE) {
                        continue;
                    }

                    if (field.getMessageType().equals(ItemStateData.getDescriptor())) {
                        if (found != null) {
                            throw new IllegalStateException();
                        }
                        found = field;
                    }
                }
                itemStateField = Optional.fromNullable(found);
            }

            return itemStateField;
        }
    }

    static final Map<Descriptor, ClassState> classes = Maps.newHashMap();

    public static <T extends GeneratedMessage> boolean isDeleted(T msg) {
        ItemStateData itemState = getItemState(msg);
        if (itemState == null) {
            return false;
        }
        return itemState.hasDeletedAt();
    }

    public static <T extends GeneratedMessage> ItemStateData getItemState(T msg) {
        FieldDescriptor field = findItemStateField(msg.getDescriptorForType());
        if (field == null) {
            return null;
        }
        try {
            return (ItemStateData) msg.getField(field);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading item state field", e);
        }
    }

    private static FieldDescriptor findItemStateField(Descriptor descriptorForType) {
        return getClassState(descriptorForType).getItemStateField().orNull();
    }

    private static ClassState getClassState(Descriptor descriptor) {
        synchronized (classes) {
            ClassState classState = classes.get(descriptor);
            if (classState == null) {
                classState = new ClassState(descriptor);
                classes.put(descriptor, classState);
            }
            return classState;
        }
    }

    public static <T extends GeneratedMessage> T markDeleted(NumberedItemCollection<T> store, T msg)
            throws CloudException {
        Message.Builder builder = ProtobufUtils.newBuilder(msg.getClass());
        builder.mergeFrom(msg);

        FieldDescriptor field = findItemStateField(msg.getDescriptorForType());
        if (field == null) {
            throw new IllegalStateException();
        }

        ItemStateData itemStateData;
        try {
            itemStateData = (ItemStateData) builder.getField(field);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading item state field", e);
        }

        if (itemStateData.hasDeletedAt()) {
            throw new IllegalStateException();
        }

        ItemStateData.Builder b = ItemStateData.newBuilder(itemStateData);
        b.setDeletedAt(Clock.getTimestamp());
        builder.setField(field, b.build());

        return store.update(builder);
    }

    public static boolean usesItemState(Descriptor descriptor) {
        return getClassState(descriptor).getItemStateField().isPresent();
    }

    public static void setUpdatedAt(Builder item) {
        Descriptor descriptor = item.getDescriptorForType();
        Optional<FieldDescriptor> itemStateField = getClassState(descriptor).getItemStateField();
        if (itemStateField.isPresent()) {
            ItemStateData itemStateData;
            try {
                itemStateData = (ItemStateData) item.getField(itemStateField.get());
            } catch (Exception e) {
                throw new IllegalArgumentException("Error reading item state field", e);
            }

            ItemStateData.Builder b = ItemStateData.newBuilder(itemStateData);
            b.setUpdatedAt(Clock.getTimestamp());
            item.setField(itemStateField.get(), b.build());
        }
    }

    public static void setCreatedAt(Builder item) {
        Descriptor descriptor = item.getDescriptorForType();
        Optional<FieldDescriptor> itemStateField = getClassState(descriptor).getItemStateField();
        if (itemStateField.isPresent()) {
            ItemStateData itemStateData;
            try {
                itemStateData = (ItemStateData) item.getField(itemStateField.get());
            } catch (Exception e) {
                throw new IllegalArgumentException("Error reading item state field", e);
            }

            ItemStateData.Builder b = ItemStateData.newBuilder(itemStateData);
            b.setCreatedAt(Clock.getTimestamp());
            item.setField(itemStateField.get(), b.build());
        }
    }

}
