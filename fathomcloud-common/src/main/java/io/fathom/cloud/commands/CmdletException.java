package io.fathom.cloud.commands;

/**
 * An exception that is an 'expected' failure. We should print the error message
 * and return the exit code.
 */
public class CmdletException extends Exception {
    private static final long serialVersionUID = 1L;

    final int exitCode;

    public CmdletException(String message) {
        super(message);
        this.exitCode = 1;
    }

    public int getExitCode() {
        return exitCode;
    }

}
