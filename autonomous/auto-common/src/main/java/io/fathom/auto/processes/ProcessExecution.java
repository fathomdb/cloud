package io.fathom.auto.processes;

public class ProcessExecution {
    final Integer exitCode;
    final String stdout;
    final String stderr;

    public ProcessExecution(Integer exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public boolean didExit() {
        return exitCode != null;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

}
