package io.fathom.cloud.network;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

public class ProtobufFilter {
    final FieldDescriptor field;
    final Object find;

    public ProtobufFilter(FieldDescriptor field, Object find) {
        super();
        this.field = field;
        this.find = find;
    }

    public boolean matches(Message m) {
        Object o = m.getField(field);
        if (find.equals(o)) {
            return true;
        }
        return false;
    }
}
