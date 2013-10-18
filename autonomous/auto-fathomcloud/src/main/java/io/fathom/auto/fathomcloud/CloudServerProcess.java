package io.fathom.auto.fathomcloud;

import io.fathom.auto.TimeSpan;
import io.fathom.auto.fathomcloud.config.ConfigurationFileTemplate;
import io.fathom.auto.fathomcloud.config.LogConfigurationTemplate;
import io.fathom.auto.processes.Pid;
import io.fathom.auto.processes.ProcFs;
import io.fathom.auto.processes.ProcessExecution;
import io.fathom.auto.processes.Processes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class CloudServerProcess {
    private static final String MAIN_CLASS_NAME = "io.fathom.cloud.CloudServer";

    private static final Logger log = LoggerFactory.getLogger(CloudServerProcess.class);

    private final Pid pid;

    final File instanceDir;

    private final FathomCloudConfig config;

    final File installDir;

    public CloudServerProcess(FathomCloudConfig config, Pid pid) {
        this.instanceDir = config.getInstanceDir();
        this.installDir = config.getInstallDir();
        this.config = config;
        this.pid = pid;
    }

    static File getPidFile(File instanceDir) {
        return new File(instanceDir, "fathomcloud.pid");
    }

    static File getLogFile(File instanceDir) {
        return new File(instanceDir, "fathomcloud.log");
    }

    static File getConfigFile(File instanceDir) {
        return new File(instanceDir, "configuration.properties");
    }

    static File getLogConfigFile(File instanceDir) {
        return new File(instanceDir, "logback.xml");
    }

    public static CloudServerProcess start(FathomCloudConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();

        configureInstance(config);

        ProcessBuilder pb = buildLauncherProcess(config);

        ProcessExecution execution = Processes.run(pb, TimeSpan.minutes(1));

        if (!execution.didExit()) {
            throw new IOException("Timeout while starting Process");
        } else {
            if (execution.getExitCode() == 0) {
                log.info("Process started OK");

                // TODO: Poll loop with timeout?
                TimeSpan.seconds(2).sleep();

                File pidFile = getPidFile(instanceDir);

                Pid pid = Pid.read(pidFile);
                if (pid == null) {
                    throw new IOException("Process started, but could not read pid from file");
                }

                return new CloudServerProcess(config, pid);
            } else {
                throw new IOException("Error starting Process process");
            }
        }
    }

    private static void configureInstance(FathomCloudConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();

        {
            ConfigurationFileTemplate template = new ConfigurationFileTemplate();
            template.write(getConfigFile(instanceDir), config);
        }

        {
            LogConfigurationTemplate template = new LogConfigurationTemplate();
            template.write(getLogConfigFile(instanceDir), config);
        }
    }

    private static ProcessBuilder buildLauncherProcess(FathomCloudConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();

        File logFile = getLogFile(instanceDir);
        File pidFile = getPidFile(instanceDir);

        File tmpDir = new File("/tmp");

        // Be sure we don't end up as the process parent...
        File trampolineScript = new File(tmpDir, "run-fathomcloud.sh");

        {
            StringWriter stringWriter = new StringWriter();
            try (PrintWriter s = new PrintWriter(stringWriter)) {
                s.println("#!/bin/bash");
                s.println("");

                List<String> args = Lists.newArrayList();

                args.add("su");
                args.add("fathomcloud");
                args.add("-c");
                args.add("\"java");
                args.add("-cp '/opt/fathomcloud/lib/*'");
                args.add("-Dconf=/var/fathomcloud/configuration.properties");
                args.add("-Dlogback.configurationFile=/var/fathomcloud/logback.xml");
                args.add("-Dzookeeper.jmx.log4j.disable=true");
                args.add(MAIN_CLASS_NAME + "\"");
                args.add(">");
                args.add(logFile.getAbsolutePath());
                args.add("2>&1");
                args.add("&");

                s.println(Joiner.on(" ").join(args));

                s.println("pid=$!");
                s.println("echo ${pid} > " + pidFile.getAbsolutePath());
            }

            Files.write(stringWriter.toString(), trampolineScript, Charsets.UTF_8);
        }

        List<String> args = Lists.newArrayList();
        args.add("/bin/bash"); // Avoids need to chmod
        args.add(trampolineScript.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(args);
        return pb;
    }

    public static CloudServerProcess find(FathomCloudConfig config) throws IOException {
        File pidFile = getPidFile(config.getInstanceDir());

        Pid pid = Pid.read(pidFile);
        if (pid == null) {
            return null;
        }

        CloudServerProcess process = new CloudServerProcess(config, pid);

        if (!process.isRunning()) {
            log.info("Found process in pid file, but was not our process: {}", pid);

            pidFile.delete();
            process = null;
        } else {
            log.info("Found existing process: {}", pid);
        }

        return process;
    }

    public boolean isRunning() throws IOException {
        ProcFs.Process process = ProcFs.findProcess(pid);
        if (process == null) {
            log.info("Process went away (no process with pid)");
            return false;
        }
        List<String> cmdline = process.getCmdline();
        return isOurProcess(cmdline);
    }

    private boolean isOurProcess(List<String> cmdline) {
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
        if (c.indexOf(MAIN_CLASS_NAME) == -1) {
            log.info("Process went away (cmdline is {})", c);
            return false;
        }

        return true;
    }
}
