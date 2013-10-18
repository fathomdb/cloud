package io.fathom.auto.processes;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;

public class Pid {

    private static final Logger log = LoggerFactory.getLogger(Pid.class);

    final long pid;

    public Pid(long pid) {
        this.pid = pid;
    }

    public static Pid read(File pidFile) throws IOException {
        String pidString = null;
        Long pid = null;

        if (pidFile.exists()) {
            pidString = Files.toString(pidFile, Charsets.UTF_8);
        }

        if (!Strings.isNullOrEmpty(pidString)) {
            pidString = pidString.trim();

            try {
                pid = Long.valueOf(pidString);
            } catch (NumberFormatException e) {
                log.warn("Error parsing pid file contents: {}", pidString);
            }
        }
        if (pid == null) {
            return null;
        }
        return new Pid(pid);
    }

    public long getValue() {
        return pid;
    }

    @Override
    public String toString() {
        return "Pid[" + pid + "]";
    }

}
