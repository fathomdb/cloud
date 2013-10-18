package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

abstract class FieldMapper {
    public final FieldDescriptor field;
    public final String jsonName;

    public FieldMapper(FieldDescriptor field) {
        this.field = field;
        this.jsonName = field.getName();
    }

    public void write(Message src, ProtobufWriter json) throws IOException {
        if (src.hasField(field)) {
            Object value = src.getField(field);

            if (value != null) {
                json.name(jsonName);
                writeValue(value, json);
            }
        }
    }

    public abstract void writeValue(Object o, ProtobufWriter json) throws IOException;

    public void read(Builder dest, JsonReader json) throws IOException {
        switch (json.peek()) {
        case NULL:
            json.nextNull();
            break;

        default:
            Object o = readValue(json);
            dest.setField(field, o);
            break;
        }
    }

    public abstract Object readValue(JsonReader json) throws IOException;
}
