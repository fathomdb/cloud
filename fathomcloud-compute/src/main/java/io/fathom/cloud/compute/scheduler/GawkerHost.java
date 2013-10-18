package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.compute.actions.ApplydContext;
import io.fathom.cloud.compute.actions.ConfigureFirewall;
import io.fathom.cloud.compute.actions.ConfigureIpset;
import io.fathom.cloud.compute.actions.ConfigureVirtualIp;
import io.fathom.cloud.compute.actions.network.VirtualIpMapper;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.LxcConfigBuilder.Volume;
import io.fathom.cloud.compute.scheduler.SshCommand.SshCommandExecution;
import io.fathom.cloud.compute.services.DatacenterManager;
import io.fathom.cloud.protobuf.CloudModel.FlavorData;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.services.ImageService;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.RemoteTempFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SftpChannel.WriteMode;
import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.gson.Gson;

public class GawkerHost extends SchedulerHost {
    private static final Logger log = LoggerFactory.getLogger(InstanceScheduler.class);

    private final SshConfig sshConfig;

    private final ImmutableList<SchedulerHostNetwork> networks;

    private final File secretsDir;

    private final DatacenterManager datacenter;

    public GawkerHost(DatacenterManager datacenter, HostData hostInfo, SshConfig sshConfig) {
        super(hostInfo);
        this.datacenter = datacenter;
        this.sshConfig = sshConfig;

        this.networks = buildNetworks();

        this.secretsDir = new File("/var/fathomcloud/secrets/containers/");
    }

    @Override
    public ConfigurationOperation startConfiguration() throws CloudException {
        return new GawkerConfigurationOperation();
    }

    class GawkerConfigurationOperation implements ConfigurationOperation {
        private boolean updateApplyd = false;
        private final Sftp sftp;
        private final ApplydContext applydContext;

        public GawkerConfigurationOperation() throws CloudException {
            this.sftp = buildSftp();
            this.applydContext = new ApplydContext(sftp);
        }

        @Override
        public void configureFirewall(InstanceData instance, List<SecurityGroupData> securityGroups)
                throws CloudException {
            ConfigureFirewall configureFirewall = new ConfigureFirewall(GawkerHost.this, applydContext);

            // Write the security groups first, in case we have an error or
            // concurrent operation
            // (instances rules depend on security groups)
            for (SecurityGroupData securityGroup : securityGroups) {
                updateApplyd |= configureFirewall.updateConfig(securityGroup);
            }

            if (instance != null) {
                updateApplyd |= configureFirewall.updateConfig(instance);
            }
        }

        @Override
        public boolean applyChanges() throws CloudException {
            if (updateApplyd) {
                applydContext.apply(sshConfig);
                updateApplyd = false;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            sftp.close();
        }

        @Override
        public void removeFirewallConfig(InstanceData instance) throws CloudException {
            ConfigureFirewall configureFirewall = new ConfigureFirewall(GawkerHost.this, applydContext);
            updateApplyd |= configureFirewall.removeConfig(instance);
        }

        @Override
        public void attachVip(InstanceData instance, VirtualIp vip) throws CloudException {
            GawkerHost host = GawkerHost.this;

            VirtualIpMapper mapper = VirtualIpMapper.build(host, instance, vip);
            ConfigureVirtualIp configureVip = new ConfigureVirtualIp(host, applydContext);

            String hostIp = mapper.mapIp(host, instance, vip);
            updateApplyd |= configureVip.updateConfig(instance, vip, hostIp);
        }

        @Override
        public void detachVip(InstanceData instance, VirtualIp vip) throws CloudException {
            GawkerHost host = GawkerHost.this;

            VirtualIpMapper mapper = VirtualIpMapper.build(host, instance, vip);
            ConfigureVirtualIp configureVip = new ConfigureVirtualIp(host, applydContext);

            mapper.unmapIp(host, instance, vip);
            updateApplyd |= configureVip.removeConfig(vip);
        }

        @Override
        public void configureIpset(long securityGroupId, Set<String> ips) throws CloudException {
            ConfigureIpset conf = new ConfigureIpset(GawkerHost.this, applydContext);
            updateApplyd |= conf.updateConfig(securityGroupId, ips);
        }
    }

    @Override
    public void startContainer(UUID containerId) throws CloudException {
        try (Sftp sftp = buildSftp()) {
            GawkerProcess process = new GawkerProcess();

            File processFile = getProcessFile(containerId);

            process.Name = "/usr/bin/lxc-start";

            File configDir = getConfigDir(containerId);

            List<String> args = Lists.newArrayList();
            args.add("-n");
            args.add(containerId.toString());
            args.add("-f");
            args.add(new File(configDir, "config.lxc").getAbsolutePath());
            process.Args = args;

            process.Dir = getRootFs(containerId).getAbsolutePath();

            String json = new Gson().toJson(process);
            sftp.writeAtomic(new RemoteFile(processFile), json.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new CloudException("Error starting container", e);
        }
    }

    private File getProcessFile(UUID containerId) {
        return new File("/etc/gawker/processes/vm-" + containerId);
    }

    private File getConfigDir(UUID containerId) {
        return new File("/var/fathomcloud/vms/" + containerId + "/");
    }

    private File getRootFs(UUID containerId) {
        return new File("/var/fathomcloud/rootfs/" + containerId + "/");
    }

    private File getVolumePath(VolumeType volumeType, UUID volumeId) {
        return new File("/volumes/" + volumeType.name().toLowerCase() + "/" + volumeId + "/");
    }

    private Sftp buildSftp() throws CloudException {
        return buildSftp(getSystemTempDir());
    }

    private Sftp buildSftp(RemoteFile tempDir) throws CloudException {
        SftpChannel sftpChannel;
        try {
            sftpChannel = sshConfig.getSftpChannel();
        } catch (IOException e) {
            throw new CloudException("Error connecting to host", e);
        }
        return new Sftp(sftpChannel, tempDir);
    }

    @Override
    public UUID createContainer(InstanceData instance, ImageService.Image image) throws CloudException {
        UUID containerId = UUID.randomUUID();

        {
            ContainerInfo container = new ContainerInfo();
            container.imageId = image.getUniqueKey();
            container.key = containerId.toString();

            LxcConfigBuilder lxcConfig = new LxcConfigBuilder();

            lxcConfig.hostname = "s" + containerId;
            lxcConfig.bridge = "virbr0";
            lxcConfig.rootfs = getRootFs(containerId).getAbsolutePath();

            lxcConfig.configDir = getConfigDir(containerId).getAbsolutePath();

            lxcConfig.memoryLimitMB = 1024;
            lxcConfig.swapMemoryLimitMB = lxcConfig.memoryLimitMB;
            lxcConfig.cpuShares = 128;

            if (instance.hasFlavor()) {
                FlavorData flavor = instance.getFlavor();
                if (flavor.hasRam()) {
                    lxcConfig.memoryLimitMB = flavor.getRam();
                }

                lxcConfig.swapMemoryLimitMB = lxcConfig.memoryLimitMB;
                if (flavor.hasSwap()) {
                    lxcConfig.swapMemoryLimitMB += flavor.getSwap();
                }

                if (flavor.hasVcpus()) {
                    lxcConfig.cpuShares *= flavor.getVcpus();
                }
            }

            List<NetworkAddressData> addresses = instance.getNetwork().getAddressesList();
            if (addresses != null && !addresses.isEmpty()) {
                NetworkAddressData bestIpv4 = null;
                NetworkAddressData bestIpv6 = null;

                for (NetworkAddressData address : addresses) {
                    InetAddress inetAddress = InetAddresses.forString(address.getIp());
                    if (inetAddress instanceof Inet4Address) {
                        if (bestIpv4 == null) {
                            bestIpv4 = address;
                        } else {
                            log.warn("Cannot choose between IPv4 addresses");
                        }
                    } else if (inetAddress instanceof Inet6Address) {
                        if (bestIpv6 == null) {
                            bestIpv6 = address;
                        } else {
                            log.warn("Cannot choose between IPv6 addresses");
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                }
                if (bestIpv4 != null) {
                    lxcConfig.ipv4Gateway = bestIpv4.getGateway();
                    lxcConfig.ipv4 = bestIpv4.getIp() + "/" + bestIpv4.getPrefixLength();
                }

                if (bestIpv6 != null) {
                    lxcConfig.ipv6Gateway = bestIpv6.getGateway();
                    lxcConfig.ipv6 = bestIpv6.getIp() + "/" + bestIpv6.getPrefixLength();
                }

                if (bestIpv6 != null && bestIpv6.hasMacAddress()) {
                    lxcConfig.hwaddr = bestIpv6.getMacAddress();
                } else if (bestIpv4 != null && bestIpv4.hasMacAddress()) {
                    lxcConfig.hwaddr = bestIpv4.getMacAddress();
                }
            }

            container.lxcConfig = lxcConfig;

            // container.injectFiles = Lists.newArrayList();
            // if (instance.hasKeyPair()) {
            // KeyPairData keyPair = instance.getKeyPair();
            //
            // InjectFile injectFile = new InjectFile();
            // injectFile.path = "/root/.ssh/authorized_keys";
            // injectFile.contents =
            // keyPair.getPublicKey().getBytes(Charsets.US_ASCII);
            // injectFile.mode = 0700;
            // container.injectFiles.add(injectFile);
            // }

            createContainer(containerId, container);
        }
        return containerId;
    }

    private void createContainer(UUID containerId, ContainerInfo container) throws CloudException {
        try (Sftp sftp = buildSftp()) {
            File rootfsPath = getRootFs(containerId);
            copyImageToRootfs(container.imageId, rootfsPath);

            for (VolumeType volumeType : new VolumeType[] { VolumeType.Ephemeral, VolumeType.Persistent }) {
                File path = createVolume(volumeType, containerId);

                Volume volume = new Volume();
                volume.hostPath = path.getAbsolutePath();
                volume.instancePath = "/volumes/" + volumeType.name().toLowerCase();
                container.lxcConfig.volumes.add(volume);
            }

            File configDir = getConfigDir(containerId);
            sftp.mkdirs(configDir);

            // // Don't use atomic... we don't have the right tmp, and we don't
            // // need atomic yet
            // String json = new Gson().toJson(container);
            // WriteFile.with(sshConfig).from(json).to(new File(configDir,
            // "config.json")).run();

            // for (InjectFile injectFile : container.injectFiles) {
            // // It's a security issue both in terms of the container,
            // // but also it requires granting the fathomcloud user lots of
            // // permissions
            // log.warn("Injecting files is deprecated");
            //
            // File injectPath = new File(rootfsPath, injectFile.path);
            // WriteFile writer =
            // WriteFile.with(sshConfig).to(injectPath).from(injectFile.contents);
            //
            // if (injectFile.mode != 0) {
            // writer.chmod(injectFile.mode);
            // }
            //
            // writer.chown(0, 0);
            //
            // writer.withSudo().run();
            // }

            // Don't use atomic... we don't have the right tmp, and we don't
            String lxcConfig = container.lxcConfig.build();
            WriteFile.with(sshConfig).from(lxcConfig).to(new File(configDir, "config.lxc")).run();
        } catch (IOException e) {
            throw new CloudException("Error creating container", e);
        }
    }

    private void copyImageToRootfs(ImageKey imageId, File rootfsPath) throws IOException {
        RemoteFile imageVolume = getImagePath(imageId);

        List<String> cmd = Lists.newArrayList();
        cmd.add("sudo /sbin/btrfs");
        cmd.add("subvolume");
        cmd.add("snapshot");
        cmd.add(imageVolume.getSshPath().getAbsolutePath());
        cmd.add(rootfsPath.getAbsolutePath());

        SshCommand sshCommand = new SshCommand(sshConfig, Joiner.on(" ").join(cmd));
        sshCommand.run();
    }

    private File createVolume(VolumeType volumeType, UUID volumeId) throws IOException {
        File imageVolume = getVolumePath(volumeType, volumeId);

        ShellCommand cmd = ShellCommand.create("/sbin/btrfs");
        cmd.literal("subvolume");
        cmd.literal("create");
        cmd.arg(imageVolume);

        cmd.useSudo();

        SshCommand sshCommand = cmd.withSsh(sshConfig);
        sshCommand.run();

        return imageVolume;
    }

    @Override
    public boolean stopContainer(UUID containerId) throws CloudException {
        try (Sftp sftp = buildSftp()) {
            File processFile = getProcessFile(containerId);
            sftp.delete(processFile);
            return true;
        } catch (IOException e) {
            throw new CloudException("Error stopping container", e);
        }
    }

    @Override
    public boolean hasImage(ImageKey imageId) throws IOException, CloudException {
        RemoteFile imageFile = getImagePath(imageId);

        try (SftpChannel sftp = buildSftp(getImageTmpdir())) {
            return sftp.exists(imageFile.getSshPath());
        }
    }

    private RemoteFile getImagePath(ImageKey imageId) {
        RemoteFile imageDir = getImageBaseDir();
        RemoteFile imageFile = new RemoteFile(imageDir, imageId.getKey());
        return imageFile;
    }

    private RemoteFile getImageBaseDir() {
        RemoteFile imageDir = new RemoteFile(new File("/var/fathomcloud/images"));
        return imageDir;
    }

    private RemoteFile getImageTmpdir() {
        RemoteFile imageDir = getImageBaseDir();
        return new RemoteFile(imageDir, "tmp");
    }

    private RemoteFile getSystemTempDir() {
        return new RemoteFile(new File("/tmp"));
    }

    @Override
    public void uploadImage(ImageKey imageId, BlobData imageData) throws IOException, CloudException {
        // TODO: Support side-load
        // TODO: Move to script
        // TODO: Delete tempImageId on fail
        ImageKey tempImageId = new ImageKey(UUID.randomUUID().toString());

        try (Sftp sftp = buildSftp(getImageTmpdir())) {
            sftp.mkdirs(getImageTmpdir().getSshPath());

            try (RemoteTempFile tar = sftp.buildRemoteTemp()) {
                try (OutputStream os = sftp.writeFile(tar.getSshPath(), WriteMode.Overwrite)) {
                    imageData.copyTo(os);
                }

                RemoteFile tempVolume = getImagePath(tempImageId);

                {
                    String cmd = "sudo btrfs subvolume create " + tempVolume.getSshPath();
                    SshCommand sshCommand = new SshCommand(sshConfig, cmd);
                    sshCommand.run();
                }

                {
                    String cmd = "sudo tar --numeric-owner -f " + tar.getSshPath() + " -C " + tempVolume.getSshPath()
                            + " -xz";
                    SshCommand sshCommand = new SshCommand(sshConfig, cmd);
                    sshCommand.run();
                }

                RemoteFile imageVolume = getImagePath(imageId);
                {
                    String cmd = "sudo btrfs subvolume snapshot -r " + tempVolume.getSshPath() + " "
                            + imageVolume.getSshPath();
                    SshCommand sshCommand = new SshCommand(sshConfig, cmd);
                    sshCommand.run();
                }
            }
        }
    }

    @Override
    public List<SchedulerHostNetwork> getNetworks() {
        return networks;
    }

    private ImmutableList<SchedulerHostNetwork> buildNetworks() {
        // We have one public IPv6 network, and one private IPv4 network
        SchedulerHostNetwork ipv4;
        SchedulerHostNetwork ipv6;

        // The IPv6 network has ::1 as the host, and ::1 acts as
        // the gateway, unless we have configured a different gateway
        {
            final IpRange ipRange = IpRange.parse(hostData.getCidr());

            final InetAddress gateway;
            if (hostData.hasGateway()) {
                gateway = InetAddresses.forString(hostData.getGateway());
            } else {
                gateway = ipRange.getAddress();
            }

            ipv6 = new SchedulerHostNetwork() {
                @Override
                public InetAddress getGateway() {
                    return gateway;
                }

                @Override
                public IpRange getIpRange() {
                    return ipRange;
                }

                @Override
                public boolean isPublicNetwork() {
                    return true;
                }

                @Override
                public String getKey() {
                    return "ipv6";
                }

                @Override
                public SchedulerHost getHost() {
                    return GawkerHost.this;
                }
            };
        }

        // The IPv4 is private, and is really only useful for NATting.
        // It is always 100.64.0.0/10; 100.64.0.1 is always the gateway.
        {
            final IpRange ipRange = IpRange.parse("100.64.0.0/10");

            final InetAddress gateway = InetAddresses.forString("100.64.0.1");

            ipv4 = new SchedulerHostNetwork() {
                @Override
                public InetAddress getGateway() {
                    return gateway;
                }

                @Override
                public IpRange getIpRange() {
                    return ipRange;
                }

                @Override
                public boolean isPublicNetwork() {
                    return false;
                }

                @Override
                public String getKey() {
                    return "ipv4-nat";
                }

                @Override
                public SchedulerHost getHost() {
                    return GawkerHost.this;
                }
            };
        }

        return ImmutableList.of(ipv6, ipv4);
    }

    @Override
    public byte[] getSecret(UUID containerId, String key) throws IOException, CloudException {
        File containerDir = new File(secretsDir, containerId.toString());
        File secretFile = new File(containerDir, key);

        try (Sftp sftp = buildSftp()) {
            byte[] data = sftp.readAllBytes(secretFile);
            return data;
        }
    }

    @Override
    public void setSecret(UUID containerId, String key, byte[] data) throws IOException, CloudException {
        File containerDir = new File(secretsDir, containerId.toString());
        File secretFile = new File(containerDir, key);

        try (Sftp sftp = buildSftp()) {
            sftp.mkdirs(containerDir);

            WriteFile.with(sshConfig).from(data).to(secretFile).run();
        }
    }

    @Override
    public TempFile createImage(UUID containerId) throws IOException, CloudException {
        // TODO: Move to btrfs
        // TODO: Move to script

        File lxcPath = new File("/cgroup/lxc");

        LxcFreezer freezer = new LxcFreezer(lxcPath, containerId);
        freezer.setFrozen(true);

        try {
            String filename = UUID.randomUUID().toString() + ".tar.gz";
            File snapshotFile = new File(getImageTmpdir().getSshPath(), filename);

            File rootfs = getRootFs(containerId);

            String command = String.format("sudo tar -c -z -C %s -f %s .", rootfs.getAbsolutePath(),
                    snapshotFile.getAbsolutePath());

            SshCommand sshCommand = new SshCommand(sshConfig, command);
            sshCommand.run();

            // We can unfreeze the VM now
            freezer.setFrozen(false);

            // TODO: Support side-load

            TempFile tempFile = TempFile.create();
            try {
                try (Sftp sftp = buildSftp(getImageTmpdir())) {
                    sftp.copy(new RemoteFile(snapshotFile), tempFile.getFile());

                    sftp.delete(snapshotFile);
                }

                TempFile ret = tempFile;
                tempFile = null;
                return ret;
            } finally {
                if (tempFile != null) {
                    tempFile.close();
                }
            }
        } finally {
            if (freezer.isFrozen()) {
                freezer.setFrozen(false);
            }
        }
    }

    class LxcFreezer {
        final File lxcCgroups;
        final UUID containerId;

        boolean frozen;

        public LxcFreezer(File lxcCgroups, UUID containerId) {
            this.lxcCgroups = lxcCgroups;
            this.containerId = containerId;
        }

        File getFreezerFile() {
            File containerPath = new File(lxcCgroups, containerId.toString());
            File freezerFile = new File(containerPath, "freezer.state");
            return freezerFile;
        }

        void setFrozen(boolean frozen) throws IOException, CloudException {
            File freezerFile = getFreezerFile();
            String s = (frozen ? "FROZEN" : "THAWED");

            int maxAttempts = 10;

            SshCommand writeCommand = new SshCommand(sshConfig, String.format("echo '%s' | sudo tee %s", s,
                    freezerFile.getAbsolutePath()));
            SshCommand readCommand = new SshCommand(sshConfig, String.format("sudo cat %s",
                    freezerFile.getAbsolutePath()));

            int attempt = 0;
            while (true) {
                if (attempt > maxAttempts) {
                    throw new IllegalStateException("Unable to change freeze/thaw state of container");
                }

                writeCommand.run();

                // try (OutputStream os = sftp.writeFile(freezerFile,
                // WriteMode.Overwrite)) {
                // os.write(s.getBytes(Charsets.ISO_8859_1));
                // }

                TimeSpan.fromMilliseconds(100).doSafeSleep();

                SshCommandExecution readExecution = readCommand.run();

                String newStateString = readExecution.getStdout();
                if (newStateString.trim().equalsIgnoreCase(s)) {
                    this.frozen = frozen;
                    return;
                }

                attempt++;
            }
        }

        public boolean isFrozen() {
            return frozen;
        }

    }

    @Override
    public void purgeInstance(UUID containerId) throws IOException, CloudException {
        // TODO: Move to script?

        // TODO: Check if running??

        {
            // TODO: Delete snapshots??
            File dir = getRootFs(containerId);
            String command = String.format("sudo btrfs subvolume ", dir.getAbsolutePath());

            SshCommand sshCommand = new SshCommand(sshConfig, command);
            sshCommand.run();
        }

        {
            File dir = getConfigDir(containerId);
            String command = String.format("sudo rm -rf %s", dir.getAbsolutePath());

            SshCommand sshCommand = new SshCommand(sshConfig, command);
            sshCommand.run();
        }

    }

    @Override
    public DatacenterManager getDatacenterManager() {
        return datacenter;
    }

    @Override
    public String fetchUrl(URI uri) throws IOException {
        ShellCommand shellCommand = ShellCommand.create("/usr/bin/wget", "-q", "-O", "-");
        shellCommand.argQuoted(uri.toString());

        SshCommand sshCommand = shellCommand.withSsh(sshConfig);
        SshCommandExecution execution = sshCommand.run();

        return execution.getStdout();
    }
}
