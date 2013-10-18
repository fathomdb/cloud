package io.fathom.cloud.protobuf;

import io.fathom.cloud.protobuf.mapper.MessageMapper;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

public class ProtobufJsonReader {

    public static void deserialize(Message.Builder dest, JsonReader json) throws IOException {
        Descriptor descriptor = dest.getDescriptorForType();
        MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
        mapper.read(dest, json);
    }

}
