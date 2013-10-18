package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.ssh.SshConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class SshCommand {
    private static final Logger log = LoggerFactory.getLogger(SshCommand.class);

    final SshConfig sshConfig;
    final String command;

    public SshCommand(SshConfig sshConfig, String command) {
        super();
        this.sshConfig = sshConfig;
        this.command = command;
    }

    static class SshCommandExecution {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;

        public String getStderr() {
            return asString(stderr);
        }

        public String getStdout() {
            return asString(stdout);
        }

        private static String asString(ByteArrayOutputStream baos) {
            byte[] bytes = baos.toByteArray();
            return new String(bytes, Charsets.UTF_8);
        }

    }

    public SshCommandExecution run() throws IOException {
        SshCommandExecution execution = new SshCommandExecution();

        try {
            log.debug("Running command over SSH: {}", command);

            execution.exitCode = sshConfig.execute(command, execution.stdout, execution.stderr);
        } catch (IOException e) {
            throw new IOException("Error running command", e);
        }

        if (execution.exitCode != 0) {
            log.warn("command returned with exit code: {}", execution.exitCode);
            log.warn("stdout: {}", execution.getStdout());
            log.warn("stderr: {}", execution.getStderr());
            throw new IOException("Error running command. Exit code=" + execution.exitCode);
        }

        return execution;
    }

}
