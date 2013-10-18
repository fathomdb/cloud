package io.fathom.cloud.state;

import io.fathom.cloud.protobuf.ProtobufUtils;
import io.fathom.cloud.state.StateStore.StateNode;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;

public abstract class CollectionBuilderBase<T> {
    final StateNode parentNode;
    final Class<T> protobufClass;
    protected Integer idFieldNumber;
    protected Integer keyFieldNumber;
    protected final Builder template;

    public CollectionBuilderBase(StateNode parentNode, Class<T> protobufClass) {
        this.parentNode = parentNode;
        this.protobufClass = protobufClass;

        this.template = (GeneratedMessage.Builder) ProtobufUtils.newBuilder(protobufClass);
    }

    public CollectionBuilderBase<T> idField(int idFieldNumber) {
        this.idFieldNumber = idFieldNumber;
        return this;
    }

    public CollectionBuilderBase<T> keyField(int keyFieldNumber) {
        this.keyFieldNumber = keyFieldNumber;
        return this;
    }

    public abstract ItemCollection create();

    protected FieldDescriptor getKeyField() {
        FieldDescriptor keyField = null;
        if (keyFieldNumber != null) {
            keyField = template.getDescriptorForType().findFieldByNumber(keyFieldNumber);
        }
        return keyField;
    }

    protected FieldDescriptor getIdField(GeneratedMessage.Builder template) {
        FieldDescriptor idField = null;
        if (idFieldNumber != null) {
            idField = template.getDescriptorForType().findFieldByNumber(idFieldNumber);
        } else {
            throw new IllegalArgumentException();
        }
        return idField;
    }
}
