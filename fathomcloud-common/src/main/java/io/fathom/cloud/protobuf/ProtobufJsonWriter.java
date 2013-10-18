package io.fathom.cloud.protobuf;

import io.fathom.cloud.protobuf.mapper.MessageMapper;
import io.fathom.cloud.protobuf.mapper.ProtobufWriter;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

public class ProtobufJsonWriter implements ProtobufWriter {

    private final JsonWriter json;

    public ProtobufJsonWriter(JsonWriter json) {
        this.json = json;
    }

    public static void serialize(Message src, JsonWriter json) throws IOException {
        ProtobufJsonWriter writer = new ProtobufJsonWriter(json);

        Descriptor descriptor = src.getDescriptorForType();
        MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
        mapper.write(src, writer);
    }

    @Override
    public void beginObject() throws IOException {
        json.beginObject();
    }

    @Override
    public void endObject() throws IOException {
        json.endObject();
    }

    @Override
    public void name(String name) throws IOException {
        json.name(name);
    }

    @Override
    public void beginArray() throws IOException {
        json.beginArray();
    }

    @Override
    public void endArray() throws IOException {
        json.endArray();
    }

    @Override
    public void value(boolean v) throws IOException {
        json.value(v);
    }

    @Override
    public void value(long v) throws IOException {
        json.value(v);
    }

    @Override
    public void value(double v) throws IOException {
        json.value(v);
    }

    @Override
    public void value(String v) throws IOException {
        json.value(v);
    }

}
