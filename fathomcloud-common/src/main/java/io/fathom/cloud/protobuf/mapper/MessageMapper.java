package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

public class MessageMapper {
    final List<FieldMapper> mappers;
    final Map<String, FieldMapper> fields;
    final Descriptor descriptor;

    public MessageMapper(Descriptor descriptor, List<FieldMapper> mappers) {
        this.descriptor = descriptor;
        this.mappers = mappers;
        this.fields = Maps.newHashMap();
    }

    void build() {
        for (FieldMapper field : mappers) {
            fields.put(field.jsonName, field);
        }
    }

    public void write(Message src, ProtobufWriter json) throws IOException {
        json.beginObject();
        for (FieldMapper mapper : mappers) {
            mapper.write(src, json);
        }
        json.endObject();
    }

    public void read(Message.Builder dest, JsonReader json) throws IOException {
        json.beginObject();

        while (true) {
            switch (json.peek()) {
            case NAME:
                String name = json.nextName();
                FieldMapper field = fields.get(name);
                if (field == null) {
                    // TODO: Support ignoring fields?
                    throw new IOException("Found unhandled field: " + name);
                }
                field.read(dest, json);
                break;

            case END_OBJECT:
                json.endObject();
                return;

            default:
                throw new UnsupportedOperationException();
            }
        }
    }

    static Map<Descriptor, MessageMapper> descriptors = Maps.newHashMap();

    static synchronized MessageMapper getMessageMapper(Message message) {
        Descriptor descriptor = message.getDescriptorForType();

        return getMessageMapper(descriptor);
    }

    public static synchronized MessageMapper getMessageMapper(Descriptor descriptor) {
        MessageMapper messageMapper = descriptors.get(descriptor);
        if (messageMapper == null) {
            List<FieldMapper> mappers = Lists.newArrayList();
            // We add to map before it's finished and then build the object up.
            // A bit naughty, but needed for circular messages
            messageMapper = new MessageMapper(descriptor, mappers);
            descriptors.put(descriptor, messageMapper);

            for (FieldDescriptor field : descriptor.getFields()) {
                FieldMapper fieldMapper = buildMapper(field);
                mappers.add(fieldMapper);
            }

            messageMapper.build();
        }
        return messageMapper;
    }

    private static FieldMapper buildMapper(FieldDescriptor field) {
        if (field.isRepeated()) {
            FieldMapper mapper = buildFieldMapper0(field, field.getType());
            return new RepeatedFieldMapper(field, mapper);
        } else {
            return buildFieldMapper0(field, field.getType());
        }
    }

    private static FieldMapper buildFieldMapper0(FieldDescriptor field, Type fieldType) {
        switch (fieldType) {
        case MESSAGE: {
            Descriptor fieldDescriptor = field.getMessageType();
            MessageMapper mapper = getMessageMapper(fieldDescriptor);
            return new MessageFieldMapper(field, mapper);
        }

        case BOOL:
            return new BooleanFieldMapper(field);

        case STRING:
            return new StringFieldMapper(field);

        case INT32:
        case INT64:
        case FIXED32:
        case FIXED64:
        case UINT32:
        case UINT64:
        case SFIXED32:
        case SFIXED64:
        case SINT32:
        case SINT64:
            return new IntegerFieldMapper(field);

        case DOUBLE:
        case FLOAT:
            return new FloatFieldMapper(field);

        case BYTES:
            return new BytesFieldMapper(field);

        case ENUM:
            return new EnumFieldMapper(field);

        default:
            throw new UnsupportedOperationException("Unhandled field type: " + fieldType);
        }
    }

}