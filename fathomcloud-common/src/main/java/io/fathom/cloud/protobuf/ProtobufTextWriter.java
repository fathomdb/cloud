package io.fathom.cloud.protobuf;

import io.fathom.cloud.protobuf.mapper.ProtobufWriter;

import java.io.IOException;
import java.util.LinkedList;

import com.google.common.collect.Lists;

public abstract class ProtobufTextWriter implements ProtobufWriter {

    protected final LinkedList<State> keyStack = Lists.newLinkedList();

    enum StateType {
        OBJECT, ARRAY, NAME
    }

    static class State {
        StateType type;
        String key;

        int index;

        public State(StateType type, String key) {
            super();
            this.type = type;
            this.key = key;
        }
    }

    private void wroteValue() {
        State state = keyStack.peek();
        switch (state.type) {
        case NAME:
            keyStack.pop();
            break;
        case ARRAY:
            // We leave it on the stack, but increment the count
            state.index++;
            break;

        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public void beginObject() throws IOException {
        keyStack.add(new State(StateType.OBJECT, null));
    }

    @Override
    public void endObject() throws IOException {
        State state = keyStack.pop();
        if (state.type != StateType.OBJECT) {
            throw new IllegalStateException();
        }
        wroteValue();
    }

    @Override
    public void name(String name) throws IOException {
        assert name != null;
        keyStack.add(new State(StateType.NAME, name));
    }

    @Override
    public void beginArray() throws IOException {
        keyStack.add(new State(StateType.ARRAY, null));
    }

    @Override
    public void endArray() throws IOException {
        State state = keyStack.pop();
        if (state.type != StateType.ARRAY) {
            throw new IllegalStateException();
        }
        wroteValue();
    }

    @Override
    public void value(boolean v) throws IOException {
        writePath();
        writeValue(Boolean.toString(v));
        wroteValue();
    }

    @Override
    public void value(long v) throws IOException {
        writePath();
        writeValue(Long.toString(v));
        wroteValue();
    }

    protected abstract void writeValue(String s) throws IOException;

    @Override
    public void value(double v) throws IOException {
        writePath();
        writeValue(Double.toString(v));
        wroteValue();
    }

    @Override
    public void value(String v) throws IOException {
        if (v != null) {
            writePath();
            writeValue(v);
        }
        wroteValue();
    }

    protected abstract void writePath() throws IOException;
}
