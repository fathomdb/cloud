package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SshConfig;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public abstract class HostFilesystem {

    protected final SshConfig sshConfig;

    public HostFilesystem(SshConfig sshConfig) {
        this.sshConfig = sshConfig;
    }

    public File getRootFs(UUID containerId) {
        return new File("/var/fathomcloud/rootfs/" + containerId + "/");
    }

    protected Sftp buildSftp() throws CloudException {
        return buildSftp(getImageTmpdir());
    }

    protected Sftp buildSftp(RemoteFile tempDir) throws CloudException {
        SftpChannel sftpChannel;
        try {
            sftpChannel = sshConfig.getSftpChannel();
        } catch (IOException e) {
            throw new CloudException("Error connecting to host", e);
        }
        return new Sftp(sftpChannel, tempDir);
    }

    protected RemoteFile getImagePath(ImageKey imageId) {
        RemoteFile imageDir = getImageBaseDir();
        String name = imageId.getKey() + getImageExtension();
        RemoteFile imageFile = new RemoteFile(imageDir, name);
        return imageFile;
    }

    protected abstract String getImageExtension();

    protected File getVolumePath(VolumeType volumeType, UUID volumeId) {
        return new File("/volumes/" + volumeType.name().toLowerCase() + "/" + volumeId + "/");
    }

    private RemoteFile getImageBaseDir() {
        RemoteFile imageDir = new RemoteFile(new File("/var/fathomcloud/images"));
        return imageDir;
    }

    protected RemoteFile getImageTmpdir() {
        RemoteFile imageDir = getImageBaseDir();
        return new RemoteFile(imageDir, "tmp");
    }

    public abstract void uploadImage(ImageKey imageId, BlobData imageData) throws IOException, CloudException;

    public boolean hasImage(ImageKey imageId) throws IOException, CloudException {
        RemoteFile imageFile = getImagePath(imageId);

        try (SftpChannel sftp = buildSftp()) {
            return sftp.exists(imageFile.getSshPath());
        }
    }

    public abstract void copyImageToRootfs(ImageKey imageId, File rootfsPath) throws IOException;

    public abstract File createVolume(VolumeType volumeType, UUID containerId) throws IOException;

    /**
     * This is called when the image is frozen. It should prepare a snapshot as quickly as possible.
     * 
     * The actual copying of the snapshot data should be placed into Snapshot.copyToFile, if possible.
     * 
     * Cleanup of the snapshot should be done in Snapshot.close
     * 
     * @param containerId
     * @return
     * @throws IOException
     */
    public abstract Snapshot snapshotImage(UUID containerId) throws IOException;

    public interface Snapshot extends Closeable {
        TempFile copyToFile() throws IOException, CloudException;
    }

    public abstract void purgeInstance(UUID containerId) throws IOException;
}
