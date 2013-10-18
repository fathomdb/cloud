package io.fathom.cloud.storage.api.os.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.base.Charsets;

public abstract class JsonWriter implements StreamingOutput {

    private OutputStreamWriter output;

    @Override
    public void write(OutputStream os) throws IOException, WebApplicationException {
        if (this.output != null) {
            throw new IllegalStateException();
        }
        this.output = new OutputStreamWriter(os, Charsets.UTF_8);

        write0();

        this.output.flush();
    }

    protected abstract void write0() throws IOException;

    protected void writeValue(String s) throws IOException {
        output.write('\"');
        writeEscaped(s);
        output.write('\"');
    }

    protected void writeEscaped(String s) throws IOException {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);

            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                output.write(c);
            } else {
                switch (c) {
                case '.':
                case '/':
                case ':':
                case '-':
                case '_':
                case ';':
                case ' ':
                case '=':
                case '~':
                    output.write(c);
                    break;

                default:
                    throw new UnsupportedOperationException("Can't escape JSON character: " + c);
                }
            }
        }
    }

    protected void writeComma() throws IOException {
        output.write(',');
    }

    protected void writeKeyLiteral(String s) throws IOException {
        output.write('\"');
        output.write(s);
        output.write("\":");
    }

    protected void startArray() throws IOException {
        output.write('[');
    }

    protected void endArray() throws IOException {
        output.write(']');
    }

    protected void startObject() throws IOException {
        output.write('{');
    }

    protected void endObject() throws IOException {
        output.write('}');
    }

}
