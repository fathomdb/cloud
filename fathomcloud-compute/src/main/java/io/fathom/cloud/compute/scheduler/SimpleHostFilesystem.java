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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHostFilesystem extends HostFilesystem {
    private static final Logger log = LoggerFactory.getLogger(SimpleHostFilesystem.class);

    public SimpleHostFilesystem(SshConfig sshConfig) {
        super(sshConfig);
    }

    @Override
    public void copyImageToRootfs(ImageKey imageId, File rootfsPath) throws IOException {
        RemoteFile tar = getImagePath(imageId);

        ShellCommand cmd = ShellCommand.create("/bin/tar");
        cmd.literal("--numeric-owner");
        cmd.literal("-f").arg(tar.getSshPath());
        cmd.literal("-C").arg(rootfsPath);
        cmd.literal("-xz");
        cmd.useSudo();

        SshCommand sshCommand = cmd.withSsh(sshConfig);
        sshCommand.run();
    }

    @Override
    public File createVolume(VolumeType volumeType, UUID volumeId) throws IOException {
        File imageVolume = getVolumePath(volumeType, volumeId);

        ShellCommand cmd = ShellCommand.create("/bin/mkdir");
        cmd.arg(imageVolume);

        cmd.useSudo();

        SshCommand sshCommand = cmd.withSsh(sshConfig);
        sshCommand.run();

        return imageVolume;
    }

    @Override
    public Snapshot snapshotImage(UUID containerId) throws IOException {
        throw new UnsupportedOperationException();
        //
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
        //
        // }
        //
        // @Override
        // public TempFile copyToFile() throws IOException, CloudException {
        //
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

        try (Sftp sftp = buildSftp()) {
            sftp.mkdirs(getImageTmpdir().getSshPath());

            try (RemoteTempFile tar = sftp.buildRemoteTemp()) {
                try (OutputStream os = sftp.writeFile(tar.getSshPath(), WriteMode.Overwrite)) {
                    imageData.copyTo(os);
                }

                RemoteFile imageFile = getImagePath(imageId);
                tar.renameTo(imageFile);
            }
        }
    }

    @Override
    public void purgeInstance(UUID containerId) throws IOException {
        // TODO: Delete snapshots??
        File dir = getRootFs(containerId);
        String command = String.format("sudo btrfs subvolume ", dir.getAbsolutePath());

        SshCommand sshCommand = new SshCommand(sshConfig, command);
        sshCommand.run();
    }

    @Override
    protected String getImageExtension() {
        return ".tgz";
    }

}
