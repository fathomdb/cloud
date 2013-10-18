package io.fathom.cloud.state;

import java.io.IOException;

import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

public class ProtobufCodec implements Codec {
    @Override
    public ByteString serialize(Message message) {
        ByteString data = message.toByteString();
        return data;
    }

    @Override
    public Message deserialize(Builder<?> builder, ByteString data) throws IOException {
        try {
            builder.mergeFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Error deserializing data", e);
        }
        return builder.build();
    }
}
