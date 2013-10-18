package io.fathom.cloud.commands;

import io.fathom.cloud.log.LogHook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(SshCommand.class);

    final Cmdlet cmdlet;

    private Thread thread;

    private ExitCallback exitCallback;

    public SshCommand(Cmdlet cmdlet) {
        this.cmdlet = cmdlet;
    }

    public void parseArguments(String line, List<String> args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);

        String[] argsArray = args.toArray(new String[args.size()]);
        parser.parseArgument(argsArray);
    }

    @Override
    public void setInputStream(InputStream stdin) {
        cmdlet.stdin = stdin;
    }

    @Override
    public void setOutputStream(OutputStream stdout) {
        cmdlet.stdout = stdout;
    }

    @Override
    public void setErrorStream(OutputStream stderr) {
        cmdlet.stderr = stderr;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    class SshLogHook extends LogHook {

        @Override
        public void log(String loggerName, String message, List<String[]> exceptionStacks, int levelInt)
                throws IOException {
            cmdlet.logMessage(loggerName, message, exceptionStacks, levelInt);
        }

    }

    @Override
    public void start(Environment env) throws IOException {
        if (thread != null) {
            throw new IllegalStateException();
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SshLogHook hook = new SshLogHook();
                try {
                    hook.install();
                    int exitCode = cmdlet.runCommand();

                    if (exitCallback != null) {
                        exitCallback.onExit(exitCode);
                    }
                } finally {
                    hook.remove();
                }
            }
        });

        thread.start();
    }

    @Override
    public void destroy() {

    }

}
