package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;

class BooleanFieldMapper extends FieldMapper {
    public BooleanFieldMapper(FieldDescriptor field) {
        super(field);
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        json.value((boolean) o);
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        return json.nextBoolean();
    }
}
