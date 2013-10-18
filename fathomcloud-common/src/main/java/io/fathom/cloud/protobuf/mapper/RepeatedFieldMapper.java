package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

class RepeatedFieldMapper extends FieldMapper {
    private final FieldMapper mapper;

    public RepeatedFieldMapper(FieldDescriptor field, FieldMapper mapper) {
        super(field);
        this.mapper = mapper;
    }

    @Override
    public void write(Message src, ProtobufWriter json) throws IOException {
        int count = src.getRepeatedFieldCount(field);
        if (count != 0) {
            json.name(jsonName);

            json.beginArray();
            for (int i = 0; i < count; i++) {
                Object o = src.getRepeatedField(field, i);
                mapper.writeValue(o, json);
            }
            json.endArray();
        }
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void read(Message.Builder dest, JsonReader json) throws IOException {
        switch (json.peek()) {
        case NULL:
            json.nextNull();
            break;

        case BEGIN_ARRAY:
            readArray(dest, json);
            break;

        default:
            throw new IOException("Expected array");
        }
    }

    private void readArray(Message.Builder dest, JsonReader json) throws IOException {
        json.beginArray();
        while (true) {
            switch (json.peek()) {
            case NULL:
                json.nextNull();
                dest.addRepeatedField(field, null);
                break;

            case END_ARRAY:
                json.endArray();
                return;

            case BEGIN_OBJECT:
                // Special case because we need a builder
                if (mapper instanceof MessageFieldMapper) {
                    Message.Builder builder = dest.newBuilderForField(field);
                    ((MessageFieldMapper) mapper).mapper.read(builder, json);
                    dest.addRepeatedField(field, builder.build());
                } else {
                    throw new IOException("Unexpected message");
                }
                break;

            default:
                Object o = mapper.readValue(json);
                dest.addRepeatedField(field, o);
                break;
            }
        }
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        throw new UnsupportedOperationException();
    }

}
