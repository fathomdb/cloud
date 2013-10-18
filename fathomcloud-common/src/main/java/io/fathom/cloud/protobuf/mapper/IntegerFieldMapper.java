package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;

class IntegerFieldMapper extends FieldMapper {
    public IntegerFieldMapper(FieldDescriptor field) {
        super(field);
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        Number n = (Number) o;
        json.value(n.longValue());
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        return json.nextLong();
    }
}
