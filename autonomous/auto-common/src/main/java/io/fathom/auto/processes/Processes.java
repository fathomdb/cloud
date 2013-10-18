package io.fathom.auto.processes;

import io.fathom.auto.TimeSpan;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class Processes {

    private static final Logger log = LoggerFactory.getLogger(Processes.class);

    public static ProcessExecution run(ProcessBuilder pb, TimeSpan timeout) throws IOException {
        File stdoutFile = null;
        File stderrFile = null;

        Process process = null;
        try {
            stdoutFile = File.createTempFile("stdout", "log");
            stderrFile = File.createTempFile("stderr", "log");

            pb.redirectOutput(stdoutFile);
            pb.redirectError(stderrFile);

            log.info("Running process: " + Joiner.on(" ").join(pb.command()));
            process = pb.start();

            Integer exitCode = null;

            try {
                exitCode = Processes.waitForExit(process, timeout);
            } catch (TimeoutException e) {
                log.warn("Timeout while running process");
            }

            String stdout = Files.toString(stdoutFile, Charsets.UTF_8);
            String stderr = Files.toString(stderrFile, Charsets.UTF_8);

            return new ProcessExecution(exitCode, stdout, stderr);
        } finally {
            if (process != null) {
                process.destroy();
            }

            if (stdoutFile != null) {
                stdoutFile.delete();
            }

            if (stderrFile != null) {
                stderrFile.delete();
            }
        }
    }

    public static int waitForExit(Process process, TimeSpan timeout) throws TimeoutException {
        Worker worker = new Worker(process);
        worker.start();
        timeout.join(worker);

        Integer exit = worker.getExitCode();
        if (exit != null) {
            return exit;
        } else {
            throw new TimeoutException();
        }
    }

    private static class Worker extends Thread {
        private final Process process;

        private Integer exitCode;

        private Worker(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            try {
                int exitCode = process.waitFor();
                setExitCode(exitCode);
            } catch (InterruptedException ignore) {
                return;
            }
        }

        public synchronized Integer getExitCode() {
            return exitCode;
        }

        public synchronized void setExitCode(Integer exitCode) {
            this.exitCode = exitCode;
        }
    }
}
