package io.fathom.cloud.state;

import java.io.IOException;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public interface Codec {

    ByteString serialize(Message message) throws IOException;

    Message deserialize(AbstractMessage.Builder<?> builder, ByteString data) throws IOException;

}
