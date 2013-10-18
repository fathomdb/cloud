package io.fathom.auto.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class ProcFs {
    private static final Logger log = LoggerFactory.getLogger(ProcFs.class);

    static final File ROOT = new File("/proc");

    public static class Process {
        final long pid;

        public Process(long pid) {
            this.pid = pid;
        }

        List<String> cmdline;

        File getBase() {
            return new File("/proc", Long.toString(pid));
        }

        public List<String> getCmdline() throws IOException {
            if (cmdline == null) {
                File file = new File(getBase(), "cmdline");
                byte[] data;
                try {
                    data = Files.toByteArray(file);
                } catch (FileNotFoundException e) {
                    return null;
                }

                String s = new String(data, Charsets.UTF_8);
                if (s.endsWith("\0")) {
                    s = s.substring(0, s.length() - 1);
                }

                List<String> args = Lists.newArrayList(Splitter.on('\0').split(s));
                cmdline = args;
                log.debug("CmdLine {} => {}", pid, Joiner.on(",").join(args));
            }
            return cmdline;
        }

        public long getPid() {
            return pid;
        }

        public boolean exists() {
            return getBase().exists();
        }

        @Override
        public String toString() {
            return "Process [pid=" + pid + "]";
        }

    }

    public static List<Process> findProcesses() {
        List<Process> processes = Lists.newArrayList();

        for (File file : ROOT.listFiles()) {
            if (!file.isDirectory()) {
                continue;
            }
            String name = file.getName();

            if (Strings.isNullOrEmpty(name)) {
                continue;
            }

            char first = name.charAt(0);
            if (first < '0' || first > '9') {
                continue;
            }

            Long pid = null;
            try {
                pid = Long.parseLong(name);
            } catch (NumberFormatException e) {
                log.debug("Error parsing proc dir as pid: " + name);
            }
            if (pid == null) {
                continue;
            }

            Process proc = new Process(pid);
            processes.add(proc);
        }
        return processes;
    }

    public static Process findProcess(Pid pid) {
        return findProcess(pid.getValue());
    }

    public static Process findProcess(long pid) {
        Process proc = new Process(pid);
        if (!proc.exists()) {
            return null;
        }
        return proc;
    }

}
