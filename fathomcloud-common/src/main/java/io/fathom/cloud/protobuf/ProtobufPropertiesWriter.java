package io.fathom.cloud.protobuf;

import io.fathom.cloud.protobuf.mapper.MessageMapper;

import java.io.IOException;
import java.io.Writer;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

public class ProtobufPropertiesWriter extends ProtobufTextWriter {

    final Writer out;
    final char separator;

    public ProtobufPropertiesWriter(Writer out, char separator) {
        this.out = out;
        this.separator = separator;
    }

    public static void serialize(Message src, Writer out, char separator) throws IOException {
        ProtobufPropertiesWriter writer = new ProtobufPropertiesWriter(out, separator);
        Descriptor descriptor = src.getDescriptorForType();
        MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
        mapper.write(src, writer);
    }

    @Override
    protected void writePath() throws IOException {
        int i = 0;
        for (State state : keyStack) {
            String s = null;

            switch (state.type) {
            case NAME:
                s = state.key;
                break;

            case ARRAY:
                s = Integer.toString(state.index);
                break;

            case OBJECT:
                break;

            default:
                throw new IllegalStateException();
            }

            if (s != null) {
                if (i != 0) {
                    out.append('.');
                }
                out.append(s);
                i++;
            }
        }
    }

    @Override
    protected void writeValue(String s) throws IOException {
        out.append(separator);
        out.append(s);
        out.append("\n");
    }
}
