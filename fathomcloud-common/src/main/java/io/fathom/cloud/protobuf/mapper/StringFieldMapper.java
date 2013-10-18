package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;

class StringFieldMapper extends FieldMapper {
    public StringFieldMapper(FieldDescriptor field) {
        super(field);
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        json.value((String) o);
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        return json.nextString();
    }

}