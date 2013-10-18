package io.fathom.cloud.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PrintErrorCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(PrintErrorCommand.class);

    private OutputStream err;
    private ExitCallback callback;

    final String message;
    final Cmdlet cmdlet;

    public PrintErrorCommand(String message, Cmdlet cmdlet) {
        this.message = message;
        this.cmdlet = cmdlet;
    }

    @Override
    public void setInputStream(InputStream in) {

    }

    @Override
    public void setOutputStream(OutputStream out) {

    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;

    }

    @Override
    public void start(Environment env) throws IOException {
        try (PrintWriter writer = new PrintWriter(err)) {

            if (!Strings.isNullOrEmpty(message)) {
                writer.println(message);
            }

            if (cmdlet != null) {
                if (!Strings.isNullOrEmpty(message)) {
                    writer.println("");
                }
                try {
                    CmdLineParser parser = new CmdLineParser(cmdlet);
                    writer.println("Usage:");

                    parser.printUsage(writer, null);
                } catch (Exception e) {
                    log.warn("Error printing usage", e);
                }
            }
        }

        if (callback != null) {
            callback.onExit(1);
        }
    }

    @Override
    public void destroy() {

    }

}
