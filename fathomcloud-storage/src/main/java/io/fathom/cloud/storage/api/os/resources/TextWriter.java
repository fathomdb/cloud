package io.fathom.cloud.storage.api.os.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.base.Charsets;

public abstract class TextWriter implements StreamingOutput {

    protected OutputStreamWriter output;

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

}
