package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.RemoteTempFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel.WriteMode;
import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class BtrfsHostFilesystem extends HostFilesystem {
    private static final Logger log = LoggerFactory.getLogger(BtrfsHostFilesystem.class);

    public BtrfsHostFilesystem(SshConfig sshConfig) {
        super(sshConfig);
    }

    @Override
    public void copyImageToRootfs(ImageKey imageId, File rootfsPath) throws IOException {
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

    @Override
    public File createVolume(VolumeType volumeType, UUID volumeId) throws IOException {
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
    protected String getImageExtension() {
        // We create a btrfs volume for each image, so we don't put an extension on it
        return "";
    }

    @Override
    public Snapshot snapshotImage(UUID containerId) throws IOException {
        // We need to implement this using btrfs snapshots!
        throw new UnsupportedOperationException();

        // String filename = UUID.randomUUID().toString() + ".tar.gz";
        // final File snapshotFile = new File(getImageTmpdir().getSshPath(), filename);
        //
        // File rootfs = getRootFs(containerId);
        //
        // String command = String.format("sudo tar -c -z -C %s -f %s .", rootfs.getAbsolutePath(),
        // snapshotFile.getAbsolutePath());
        //
        // SshCommand sshCommand = new SshCommand(sshConfig, command);
        // sshCommand.run();
        //
        // return new Snapshot() {
        //
        // @Override
        // public void close() throws IOException {
        // DELETE SNAPSHOT
        // }
        //
        // @Override
        // public TempFile copyToFile() throws IOException, CloudException {
        // MAKE TAR FILE FROM SNAPSHOT
        // TempFile tempFile = TempFile.create();
        // try {
        // try (Sftp sftp = buildSftp()) {
        // sftp.copy(new RemoteFile(snapshotFile), tempFile.getFile());
        //
        // sftp.delete(snapshotFile);
        // }
        //
        // TempFile ret = tempFile;
        // tempFile = null;
        // return ret;
        // } finally {
        // if (tempFile != null) {
        // tempFile.close();
        // }
        // }
        // }
        // };
    }

    @Override
    public void uploadImage(ImageKey imageId, BlobData imageData) throws IOException, CloudException {
        // TODO: Support side-load
        // TODO: Move to script
        // TODO: Delete tempImageId on fail

        ImageKey tempImageId = new ImageKey(UUID.randomUUID().toString());

        try (Sftp sftp = buildSftp()) {
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
                    ShellCommand cmd = ShellCommand.create("/bin/tar");
                    cmd.literal("--numeric-owner");
                    cmd.literal("-f").arg(tar.getSshPath());
                    cmd.literal("-C").arg(tempVolume.getSshPath());
                    cmd.literal("-xz");
                    cmd.useSudo();

                    SshCommand sshCommand = cmd.withSsh(sshConfig);
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
    public void purgeInstance(UUID containerId) throws IOException {
        // TODO: Delete snapshots??
        File dir = getRootFs(containerId);
        String command = String.format("sudo btrfs subvolume delete ", dir.getAbsolutePath());

        SshCommand sshCommand = new SshCommand(sshConfig, command);
        sshCommand.run();
    }

}
