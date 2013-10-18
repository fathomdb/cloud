package io.fathom.auto.zookeeper;

import io.fathom.auto.HostsFile;
import io.fathom.auto.zookeeper.model.ZookeeperClusterRegistration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class ZookeeperConfig {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperConfig.class);

    final File instanceDir;

    final File installDir;

    public ZookeeperConfig(File installDir, File instanceDir) {
        this.installDir = installDir;
        this.instanceDir = instanceDir;
    }

    public File getConfigFile() {
        return new File(instanceDir, "zk.conf");
    }

    public void mkdirs() {
        mkdirs(instanceDir);
        mkdirs(getDataDir());
        mkdirs(getLogsDir());
    }

    private void mkdirs(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("Error doing mkdirs on: " + dir);
            }
        }
    }

    public void writeConfig(ClusterSnapshot snapshot, int serverId) throws IOException {
        StringWriter writer = new StringWriter();
        writeConfig(snapshot, serverId, new PrintWriter(writer));

        File confFile = getConfigFile();
        Files.write(writer.toString().getBytes(Charsets.UTF_8), confFile);
    }

    void writeConfig(ClusterSnapshot snapshot, int myid, PrintWriter writer) {
        ZookeeperClusterRegistration me = snapshot.servers.get(myid);

        if (me == null) {
            throw new IllegalStateException();
        }

        writer.println("peerType=" + me.type);

        writer.println("dataDir=" + getDataDir().getAbsolutePath());
        writer.println("dataLogDir=" + getLogsDir().getAbsolutePath());
        writer.println("syncLimit=2");
        writer.println("initLimit=2");

        boolean newFormat = false;
        if (!newFormat) {
            writer.println("clientPort=2181");
        }

        for (Entry<Integer, ZookeeperClusterRegistration> entry : snapshot.servers.entrySet()) {
            int serverId = entry.getKey();
            ZookeeperClusterRegistration registration = entry.getValue();

            String serverName = "zk_" + serverId;
            if (!newFormat) {
                writer.println(String.format("server.%s=%s:2182:2183", serverId, serverName));
            } else {
                writer.println(String.format("server.%s=%s:2182:2183;%s:2181", serverId, serverName, serverName));
            }
        }
    }

    private File getDataDir() {
        return new File(instanceDir, "data");
    }

    private File getLogsDir() {
        return new File(instanceDir, "logs");
    }

    public Integer readIdFile() throws IOException {
        File file = getIdFile();
        if (!file.exists()) {
            return null;
        }

        String s = Files.toString(file, Charsets.UTF_8);
        if (Strings.isNullOrEmpty(s)) {
            return null;
        }

        return Integer.valueOf(s);
    }

    public void writeIdFile(int serverId) throws IOException {
        String s = Integer.toString(serverId);
        Files.write(s.getBytes(Charsets.UTF_8), getIdFile());
    }

    private File getIdFile() {
        return new File(getDataDir(), "myid");
    }

    public File getInstanceDir() {
        return instanceDir;
    }

    public File getInstallDir() {
        return installDir;
    }

    /**
     * We write the hosts file because otherwise ZK can't cope with IPV6 (?)
     */
    public void writeHosts(ClusterSnapshot snapshot) throws IOException {
        Map<String, String> hosts = Maps.newHashMap();

        for (Entry<Integer, ZookeeperClusterRegistration> entry : snapshot.servers.entrySet()) {
            int serverId = entry.getKey();
            ZookeeperClusterRegistration registration = entry.getValue();

            String zkHost = "zk_" + serverId;
            String ip = registration.ip;

            hosts.put(zkHost, ip);
        }

        HostsFile.setHosts(hosts);
    }

}
