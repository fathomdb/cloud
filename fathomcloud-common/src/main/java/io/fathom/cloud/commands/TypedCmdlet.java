package io.fathom.cloud.commands;

import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.sshd.common.util.NoCloseOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TypedCmdlet extends Cmdlet {
    private static final Logger log = LoggerFactory.getLogger(TypedCmdlet.class);

    public TypedCmdlet(String command) {
        super(command);
    }

    @Override
    protected final void run() throws Exception {
        Object o = run0();

        if (o != null) {
            try (Writer writer = new OutputStreamWriter(new NoCloseOutputStream(stdout))) {
                YamlWriter yaml = new YamlWriter(writer);
                yaml.write(o);
            }
        }
    }

    protected abstract Object run0() throws Exception;

}
