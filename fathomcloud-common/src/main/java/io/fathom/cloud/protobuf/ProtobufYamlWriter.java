package io.fathom.cloud.protobuf;

import io.fathom.cloud.protobuf.mapper.MessageMapper;
import io.fathom.cloud.protobuf.mapper.ProtobufWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

public class ProtobufYamlWriter implements ProtobufWriter {

    final Writer out;

    public ProtobufYamlWriter(Writer out) {
        this.out = out;
    }

    public static void serialize(Message src, Writer out) throws IOException {
        ProtobufYamlWriter writer = new ProtobufYamlWriter(out);
        Descriptor descriptor = src.getDescriptorForType();
        MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
        mapper.write(src, writer);
    }

    public static void serialize(List<? extends Message> src, Writer out) throws IOException {
        ProtobufYamlWriter writer = new ProtobufYamlWriter(out);

        for (Message message : src) {
            Descriptor descriptor = message.getDescriptorForType();
            MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
            mapper.write(message, writer);
        }
    }

    enum State {
        OBJECT, ARRAY
    }

    final LinkedList<State> stateStack = Lists.newLinkedList();

    int indent;
    String name;

    @Override
    public void beginObject() throws IOException {
        if (name != null) {
            writeIndent();
            out.write(name);
            out.write(":\n");
            this.name = null;
        } else {
            writeIndent();
            out.write("-\n");
        }
        indent++;

        stateStack.push(State.OBJECT);
    }

    @Override
    public void endObject() throws IOException {
        State state = stateStack.pop();
        if (state != State.OBJECT) {
            throw new IllegalStateException();
        }
        indent--;
    }

    @Override
    public void name(String name) throws IOException {
        this.name = name;
    }

    @Override
    public void beginArray() throws IOException {
        if (name != null) {
            writeIndent();
            out.write(name);
            out.write(":\n");
            this.name = null;
        }
        indent++;

        stateStack.push(State.ARRAY);
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < indent; i++) {
            out.write("  ");
        }
    }

    @Override
    public void endArray() throws IOException {
        State state = stateStack.pop();
        if (state != State.ARRAY) {
            throw new IllegalStateException();
        }
        indent--;
    }

    @Override
    public void value(boolean v) throws IOException {
        writeValue(Boolean.toString(v));
    }

    @Override
    public void value(long v) throws IOException {
        writeValue(Long.toString(v));
    }

    @Override
    public void value(double v) throws IOException {
        writeValue(Double.toString(v));
    }

    @Override
    public void value(String v) throws IOException {
        if (v != null) {
            writeValue(v);
        }
    }

    private void writeValue(String v) throws IOException {
        writeIndent();
        State state = stateStack.peek();
        if (state != null) {
            switch (state) {
            case OBJECT:
                break;

            case ARRAY:
                out.write("- ");
                break;

            default:
                throw new IllegalStateException();
            }
        }

        if (name != null) {
            out.write(name);
            out.write(": ");
            name = null;
        }

        out.write(v);
        out.write("\n");
    }

}
