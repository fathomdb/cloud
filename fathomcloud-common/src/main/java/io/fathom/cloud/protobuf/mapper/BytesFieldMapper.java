package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

import com.google.common.io.BaseEncoding;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;

class BytesFieldMapper extends FieldMapper {
    public BytesFieldMapper(FieldDescriptor field) {
        super(field);
    }

    @Override
    public void writeValue(Object o, ProtobufWriter json) throws IOException {
        ByteString v = (ByteString) o;
        String s = BaseEncoding.base16().encode(v.toByteArray());
        json.value(s);
    }

    @Override
    public Object readValue(JsonReader json) throws IOException {
        String s = json.nextString();
        byte[] data = BaseEncoding.base16().decode(s);
        return ByteString.copyFrom(data);
    }

}