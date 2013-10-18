package io.fathom.auto;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class HostsFile {

    private static final Logger log = LoggerFactory.getLogger(HostsFile.class);

    static class Line {
        String line;

        public Line(String line) {
            this.line = line;
        }

        public boolean update(String s) {
            if (line.equals(s)) {
                return false;
            }
            log.info("Updating /etc/hosts entry: {}", line);
            this.line = s;
            return true;
        }
    }

    public static void setHosts(Map<String, String> addHosts) throws IOException {
        if (addHosts == null || addHosts.isEmpty()) {
            return;
        }

        File hostsFile = new File("/etc/hosts");

        // Build a map for the file
        String hosts = Files.toString(hostsFile, Charsets.UTF_8);
        List<Line> lines = Lists.newArrayList();
        Map<String, Line> existingHosts = Maps.newHashMap();

        for (String s : Splitter.on('\n').split(hosts)) {
            Line line = new Line(s);
            lines.add(line);
            if (s.contains("\t")) {
                int tabIndex = s.indexOf('\t');
                String host = s.substring(tabIndex + 1);
                existingHosts.put(host, line);
            }
        }

        // Update the map
        boolean dirty = false;
        for (Entry<String, String> addHost : addHosts.entrySet()) {
            String host = addHost.getKey();
            String ip = addHost.getValue();

            String line = ip + "\t" + host;
            Line existing = existingHosts.get(host);
            if (existing != null) {
                dirty |= existing.update(line);
            } else {
                existing = new Line(line);
                lines.add(existing);
                existingHosts.put(host, existing);
                log.info("Adding /etc/hosts entry: {}", line);
                dirty = true;
            }
        }

        // Rewrite the file

        if (dirty) {
            StringBuilder sb = new StringBuilder();

            for (Line line : lines) {
                sb.append(line.line);
                sb.append("\n");
            }

            // TODO: Write atomically
            Files.write(sb.toString().getBytes(Charsets.UTF_8), hostsFile);
        }
    }

}
