package io.fathom.auto.zookeeper;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.processes.Pid;
import io.fathom.auto.processes.ProcFs;
import io.fathom.auto.processes.ProcessExecution;
import io.fathom.auto.processes.Processes;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ZookeeperProcess {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperProcess.class);

    private final Pid pid;

    final ZookeeperConfig config;

    public ZookeeperProcess(ZookeeperConfig config, Pid pid) {
        this.config = config;
        this.pid = pid;
    }

    static File getPidFile(File instanceDir) {
        return new File(getDataDir(instanceDir), "zookeeper_server.pid");
    }

    private static File getDataDir(File instanceDir) {
        return new File(instanceDir, "data");
    }

    static File getLogFile(File instanceDir) {
        return new File(instanceDir, "zookeeper.out");
    }

    public static ZookeeperProcess start(ZookeeperConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();
        File installDir = config.getInstallDir();

        File startScript = new File(installDir, "bin/zkServer.sh");

        List<String> args = Lists.newArrayList();
        args.add(startScript.getAbsolutePath());
        args.add("start");
        args.add(config.getConfigFile().getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(instanceDir);

        ProcessExecution execution = Processes.run(pb, TimeSpan.minutes(1));

        if (!execution.didExit()) {
            throw new IOException("Timeout while starting zookeeper");
        } else {
            if (execution.getExitCode() == 0) {
                log.info("Zookeeper started OK");

                File pidFile = getPidFile(instanceDir);
                Pid pid = Pid.read(pidFile);
                if (pid == null) {
                    throw new IOException("Zookeeper started, but could not read pid from file");
                }

                return new ZookeeperProcess(config, pid);
            } else {
                throw new IOException("Error starting zookeeper process");
            }
        }
    }

    public static ZookeeperProcess find(ZookeeperConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();
        File pidFile = getPidFile(instanceDir);

        Pid pid = Pid.read(pidFile);
        if (pid == null) {
            log.info("No zookeeper pid");
            return null;
        }

        if (!isZookeeper(pid)) {
            log.info("Pid found, but was not zookeeper");
            return null;
        }

        log.info("Found existing zookeeper: {}", pid);
        return new ZookeeperProcess(config, pid);
    }

    public static boolean isZookeeper(Pid pid) throws IOException {
        ProcFs.Process process = ProcFs.findProcess(pid);
        if (process == null) {
            log.info("Process went away (no process with pid)");
            return false;
        }

        List<String> cmdline = process.getCmdline();
        if (cmdline == null) {
            // Process stopped
            log.info("Process went away (no process with pid)");
            return false;
        }

        if (cmdline.isEmpty()) {
            log.info("Process went away (cmdline empty)");
            return false;
        }

        String c = Joiner.on(" ").join(cmdline);
        if (c.indexOf("org.apache.zookeeper.server") == -1) {
            log.info("Process went away (cmdline is {})", c);
            return false;
        }

        log.info("Matched zookeeper process (cmdline is {})", c);

        return true;
    }

}
