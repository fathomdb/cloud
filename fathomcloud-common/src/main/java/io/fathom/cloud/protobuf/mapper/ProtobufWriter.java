package io.fathom.cloud.protobuf.mapper;

import java.io.IOException;

public interface ProtobufWriter {

    void beginObject() throws IOException;

    void endObject() throws IOException;

    void name(String name) throws IOException;

    void beginArray() throws IOException;

    void endArray() throws IOException;

    void value(boolean v) throws IOException;

    void value(long v) throws IOException;

    void value(double v) throws IOException;

    void value(String v) throws IOException;

}
