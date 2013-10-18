package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

class EnumFieldMapper extends FieldMapper {
    final EnumDescriptor enumType;

    public EnumFieldMapper(FieldDescriptor field) {
        super(field);
        this.enumType = field.getEnumType();
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        EnumValueDescriptor v = (EnumValueDescriptor) o;
        json.value(v.getName());
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        String s = json.nextString();
        return enumType.findValueByName(s);
    }

}