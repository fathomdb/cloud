package io.fathom.auto.openstack.horizon;

import io.fathom.auto.HostsFile;
import io.fathom.auto.TimeSpan;
import io.fathom.auto.openstack.horizon.config.LocalSettingsTemplate;
import io.fathom.auto.processes.Pid;
import io.fathom.auto.processes.ProcFs;
import io.fathom.auto.processes.ProcessExecution;
import io.fathom.auto.processes.Processes;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpMethod;
import io.fathom.http.HttpRequest;
import io.fathom.http.HttpResponse;
import io.fathom.http.jre.JreHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class HorizonProcess {
    private static final Logger log = LoggerFactory.getLogger(HorizonProcess.class);

    private final Pid pid;

    final File instanceDir;

    private final HorizonConfig config;

    final File installDir;

    public HorizonProcess(HorizonConfig config, Pid pid) {
        this.instanceDir = config.getInstanceDir();
        this.installDir = config.getInstallDir();
        this.config = config;
        this.pid = pid;
    }

    static File getPidFile(File instanceDir) {
        return new File(instanceDir, "horizon.pid");
    }

    static File getLogFile(File instanceDir) {
        return new File(instanceDir, "horizon.log");
    }

    static File getConfigFile(File installDir) {
        return new File(installDir, "openstack_dashboard/local/local_settings.py");
    }

    public static HorizonProcess start(HorizonConfig config) throws IOException {
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

                return new HorizonProcess(config, pid);
            } else {
                throw new IOException("Error starting Process process");
            }
        }
    }

    private static void configureInstance(HorizonConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();
        File installDir = config.getInstallDir();

        log.info("Installing new config file");

        LocalSettingsTemplate template = new LocalSettingsTemplate();
        File configFile = getConfigFile(installDir);
        template.write(configFile, config);

        HostsFile.setHosts(config.getHosts());
    }

    private static ProcessBuilder buildLauncherProcess(HorizonConfig config) throws IOException {
        File instanceDir = config.getInstanceDir();

        File logFile = getLogFile(instanceDir);
        File pidFile = getPidFile(instanceDir);

        File tmpDir = new File("/tmp");

        // Be sure we don't end up as the process owner...
        File trampolineScript = new File(tmpDir, "run-horizon.sh");

        {
            StringWriter stringWriter = new StringWriter();
            try (PrintWriter s = new PrintWriter(stringWriter)) {
                s.println("#!/bin/bash");
                s.println("");

                List<String> args = Lists.newArrayList();
                args.add("/opt/horizon/tools/with_venv.sh");
                args.add("/opt/horizon/manage.py");
                args.add("runserver");
                args.add("[::]:8080");
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

    public static HorizonProcess find(HorizonConfig config) throws IOException {
        File pidFile = getPidFile(config.getInstanceDir());

        Pid pid = Pid.read(pidFile);
        if (pid == null) {
            return null;
        }

        HorizonProcess process = new HorizonProcess(config, pid);

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
            return false;
        }
        List<String> cmdline = process.getCmdline();
        if (!isOurProcess(cmdline)) {
            return false;
        }

        // Fetch the root page; this warms-up the app
        try {
            HttpClient httpClient = JreHttpClient.create();
            URI uri = URI.create("http://127.0.0.1:8080/");
            HttpRequest httpRequest = httpClient.buildRequest(HttpMethod.GET, uri);
            try (HttpResponse response = httpRequest.doRequest()) {
                if (response.getHttpResponseCode() != 200) {
                    throw new IllegalStateException("Bad response code from page: " + response.getHttpResponseCode());
                }
            }
        } catch (Exception e) {
            // TODO: Should we restart horizon if this failed?
            log.warn("Error while fetching warm-up page", e);
        }

        return true;
    }

    private boolean isOurProcess(List<String> cmdline) {
        if (cmdline == null) {
            // Process stopped
            return false;
        }

        if (cmdline.isEmpty()) {
            return false;
        }

        if (cmdline.get(2).equals("/opt/horizon/manage.py")) {
            return true;
        }
        return false;
    }
}
