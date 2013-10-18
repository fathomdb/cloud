package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;

class FloatFieldMapper extends FieldMapper {
    public FloatFieldMapper(FieldDescriptor field) {
        super(field);
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        Number n = (Number) o;
        json.value(n.doubleValue());
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        return json.nextDouble();
    }
}