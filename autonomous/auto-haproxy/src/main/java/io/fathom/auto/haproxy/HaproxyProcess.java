package io.fathom.auto.haproxy;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.processes.ProcessExecution;
import io.fathom.auto.processes.Processes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class HaproxyProcess {
    private static final Logger log = LoggerFactory.getLogger(HaproxyProcess.class);

    private static final File LOG_FILE = new File("/var/log/haproxy.log");
    private static final File PID_FILE = new File("/var/run/haproxy");
    public static final File CONFIG_FILE = new File("/opt/haproxy/haproxy.cfg");
    private static final File HAPROXY_CMD = new File("/opt/haproxy/haproxy");

    private String pids;

    public HaproxyProcess(String pids) {
        this.pids = pids;
    }

    public static boolean validate(File configFile) throws IOException {
        log.info("Doing haproxy validate");

        ProcessBuilder pb = new ProcessBuilder(HAPROXY_CMD.getAbsolutePath(), "-c", "-f", configFile.getAbsolutePath());

        ProcessExecution execution = Processes.run(pb, TimeSpan.seconds(10));
        if (!execution.didExit()) {
            log.warn("Timeout while validating haproxy config.");
        } else {
            int exitCode = execution.getExitCode();
            if (exitCode == 0) {
                log.info("Validated config file");
                return true;
            } else {
                log.warn("Error validating haproxy config.  Exit code {}", exitCode);

                String config = Files.toString(configFile, Charsets.UTF_8);
                log.warn("Bad haproxy config: {}", config);
            }
        }

        log.warn("stdout: {}", execution.getStdout());
        log.warn("stderr: {}", execution.getStderr());

        return false;
    }

    public static HaproxyProcess start() throws IOException {
        return start(null);
    }

    private static HaproxyProcess start(Long pid) throws IOException {
        File logFile = LOG_FILE;
        File pidFile = PID_FILE;
        File configFile = CONFIG_FILE;

        List<String> args = Lists.newArrayList();
        args.add(HAPROXY_CMD.getAbsolutePath());

        // Daemon
        args.add("-D");

        // pid file
        args.add("-p");
        args.add(pidFile.getAbsolutePath());

        // config file
        args.add("-f");
        args.add(configFile.getAbsolutePath());

        // reload
        if (pid != null) {
            args.add("-sf");
            args.add(pid.toString());
        }

        ProcessBuilder pb = new ProcessBuilder(args);

        pb.redirectOutput(logFile);
        pb.redirectError(logFile);

        if (pid == null) {
            log.info("Starting haproxy: {}", Joiner.on(" ").join(args));
        } else {
            log.info("Reloading haproxy: {}", Joiner.on(" ").join(args));
        }

        Process process = pb.start();

        int exitCode = -1;
        try {
            exitCode = Processes.waitForExit(process, TimeSpan.seconds(5));
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for haproxy to daemonize");
            return null;
        }

        if (exitCode != 0) {
            log.warn("HaProxy exited with error code.  Exit code {}", exitCode);
            return null;
        }

        String pids = Files.toString(pidFile, Charsets.UTF_8);
        log.info("Started haproxy; seems to be stable.  Pids {}", pids);
        return new HaproxyProcess(pids);
    }

    public static HaproxyProcess find() throws IOException {
        if (!PID_FILE.exists()) {
            return null;
        }

        String pids = Files.toString(PID_FILE, Charsets.UTF_8);
        pids = pids.trim();
        if (Strings.isNullOrEmpty(pids)) {
            return null;
        }

        log.info("Found existing haproxy: {}", pids);

        return new HaproxyProcess(pids);
    }

    public void reload() throws IOException, InterruptedException {
        log.warn("Reloading haproxy");

        String pidString = this.pids.trim();
        if (pidString.contains("\n")) {
            pidString = pidString.substring(0, pidString.indexOf('\n'));
            pidString = pidString.trim();
        }

        long pid = Long.valueOf(pidString);

        HaproxyProcess process = HaproxyProcess.start(pid);

        this.pids = process.pids;
    }
}
