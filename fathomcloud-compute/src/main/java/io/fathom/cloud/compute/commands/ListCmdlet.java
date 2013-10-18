package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.protobuf.ProtobufYamlWriter;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.sshd.common.util.NoCloseOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public abstract class ListCmdlet extends Cmdlet {

    private static final Logger log = LoggerFactory.getLogger(TypedCmdlet.class);

    public ListCmdlet(String command) {
        super(command);
    }

    @Override
    protected final void run() throws Exception {
        List<? extends Message> o = run0();

        if (o != null) {
            try (Writer writer = new OutputStreamWriter(new NoCloseOutputStream(stdout))) {
                ProtobufYamlWriter.serialize(o, writer);
            }
        }
    }

    protected abstract List<? extends Message> run0() throws Exception;

}
