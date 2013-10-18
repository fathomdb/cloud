package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

class MessageFieldMapper extends FieldMapper {
    final MessageMapper mapper;

    public MessageFieldMapper(FieldDescriptor field, MessageMapper mapper) {
        super(field);
        this.mapper = mapper;
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        Message msg = (Message) o;
        mapper.write(msg, json);
    }

    @Override
    public void read(Message.Builder dest, JsonReader json) throws IOException {
        switch (json.peek()) {
        case NULL:
            json.nextNull();
            break;

        case BEGIN_OBJECT:
            Message.Builder builder = dest.newBuilderForField(field);
            mapper.read(builder, json);
            dest.setField(field, builder);
            break;

        default:
            throw new IOException("Expected object");
        }
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        throw new UnsupportedOperationException();
    }

}
