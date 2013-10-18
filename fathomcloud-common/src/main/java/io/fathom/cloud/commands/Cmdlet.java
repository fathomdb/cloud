package io.fathom.cloud.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.google.common.base.Charsets;

public abstract class Cmdlet {

    private static final Logger log = LoggerFactory.getLogger(Cmdlet.class);

    protected InputStream stdin;
    protected OutputStream stdout;
    protected OutputStream stderr;

    private final String command;

    public Cmdlet(String command) {
        this.command = command;
    }

    public void parseArguments(String line, List<String> args) throws Exception {
        // this.line = line;

        CmdLineParser parser = new CmdLineParser(this);

        String[] argsArray = args.toArray(new String[args.size()]);
        parser.parseArgument(argsArray);
    }

    public int runCommand() {
        int exitCode = 0;

        try {
            run();
        } catch (Exception e) {
            log.warn("Error running cmdlet", e);

            boolean printStackTrace = true;
            String message = null;
            exitCode = 1;

            if (e instanceof CmdletException) {
                printStackTrace = false;
                message = e.getMessage();
                exitCode = ((CmdletException) e).getExitCode();
            }

            try {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(stderr, Charsets.UTF_8));
                if (message != null) {
                    writer.println(message);
                }
                if (printStackTrace) {
                    e.printStackTrace(writer);
                }
                writer.flush();
            } catch (Exception e2) {
                log.warn("Error writing exception", e2);
            }
        } finally {
            try {
                stdout.flush();
            } catch (IOException e) {
                log.warn("Error flushing SSH stdout", e);
            }
            try {
                stderr.flush();
            } catch (IOException e) {
                log.warn("Error flushing SSH stderr", e);
            }
        }

        return exitCode;
    }

    protected abstract void run() throws Exception;

    protected synchronized void println(String s) throws IOException {
        stdout.write(s.getBytes(Charsets.UTF_8));
        stdout.write("\r\n".getBytes(Charsets.UTF_8));
    }

    protected synchronized void println(String format, Object... args) throws IOException {
        String msg = String.format(format, args);
        println(msg);
    }

    protected synchronized void flush() throws IOException {
        stdout.flush();
        stderr.flush();
    }

    public boolean accepts(String command) {
        return this.command.equals(command);
    }

    public String getCommand() {
        return command;
    }

    public void logMessage(String loggerName, String message, List<String[]> exceptionStacks, int levelInt)
            throws IOException {
        if (loggerName.startsWith("org.apache.sshd.")) {
            return;
        }

        if (levelInt <= Level.DEBUG_INT) {
            return;
        }

        if (message != null) {
            // stdout.write(loggerName.getBytes(Charsets.UTF_8));
            // stdout.write("\t".getBytes(Charsets.UTF_8));
            stderr.write(message.getBytes(Charsets.UTF_8));
            stderr.write("\n".getBytes(Charsets.UTF_8));

            if (exceptionStacks != null) {
                if (!exceptionStacks.isEmpty()) {
                    String[] exceptionStack = exceptionStacks.get(0);
                    if (exceptionStack != null && exceptionStack.length != 0) {
                        String line = exceptionStack[0];
                        stderr.write("\t".getBytes(Charsets.UTF_8));
                        stderr.write(line.getBytes(Charsets.UTF_8));
                        stderr.write("\n".getBytes(Charsets.UTF_8));
                    }
                }

                // TODO: Support verbose mode?
                // for (String[] exceptionStack : exceptionStacks) {
                // for (String line : exceptionStack) {
                // stderr.write(line.getBytes(Charsets.UTF_8));
                // stderr.write("\n".getBytes(Charsets.UTF_8));
                // }
                // }
            }

            stderr.flush();
        }
    }

}
