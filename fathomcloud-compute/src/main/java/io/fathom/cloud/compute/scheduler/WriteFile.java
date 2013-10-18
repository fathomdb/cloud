package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.RemoteTempFile;
import io.fathom.cloud.ssh.SftpChannel;
import io.fathom.cloud.ssh.SshConfig;
import io.fathom.cloud.ssh.SftpChannel.WriteMode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

public class WriteFile {

    private final SshConfig sshConfig;

    private File dest;
    private ByteSource source;
    private Integer chmod;
    private Integer chownUser;
    private Integer chownGroup;
    private boolean sudo;
    private boolean atomic;

    private RemoteFile atomicTempPath;

    WriteFile(SshConfig sshConfig) {
        this.sshConfig = sshConfig;
    }

    public static WriteFile with(SshConfig sshConfig) {
        return new WriteFile(sshConfig);
    }

    public WriteFile to(File dest) {
        this.dest = dest;
        return this;
    }

    public WriteFile from(byte[] data) {
        this.source = ByteSource.wrap(data);
        return this;
    }

    public WriteFile from(String s) {
        return from(s.getBytes(Charsets.UTF_8));
    }

    public WriteFile chmod(int mode) {
        this.chmod = mode;
        return this;
    }

    // chown and chgrp require CAP_CHOWN / root
    // It's a huge backdoor
    @Deprecated
    public WriteFile chown(int user, int group) {
        this.chownUser = user;
        this.chownGroup = group;
        return this;
    }

    public WriteFile withSudo() {
        this.sudo = true;
        return this;
    }

    public WriteFile atomic(RemoteFile tempPath) {
        this.atomicTempPath = tempPath;
        this.atomic = true;
        return this;
    }

    public void run() throws IOException {
        if (dest == null || source == null) {
            throw new IllegalStateException();
        }

        try (SftpChannel sftp = sshConfig.getSftpChannel()) {
            RemoteTempFile tempFile = null;
            File remoteDest = dest;

            try {
                if (sudo || atomic) {
                    if (atomicTempPath != null) {
                        tempFile = RemoteTempFile.create(sftp, atomicTempPath);
                    } else {
                        tempFile = RemoteTempFile.create(sftp, new RemoteFile(new File("/tmp")));
                    }
                    remoteDest = tempFile.getSshPath();
                }

                try (OutputStream os = sftp.writeFile(remoteDest, WriteMode.Overwrite)) {
                    source.copyTo(os);
                }

                if (chmod != null) {
                    sftp.chmod(remoteDest, chmod);
                }

                // chown and chgrp require CAP_CHOWN / root
                if (chownGroup != null) {
                    sftp.chgrp(remoteDest, chownGroup);
                }

                if (chownUser != null) {
                    sftp.chown(remoteDest, chownUser);
                }

                if (tempFile != null) {
                    if (atomic) {
                        if (sudo) {
                            throw new UnsupportedOperationException("Atomic sudo move not yet implemented");
                        }

                        tempFile.renameTo(new RemoteFile(dest));
                    } else {
                        ShellCommand command = ShellCommand.create("/bin/cp");

                        command.arg(remoteDest);
                        command.arg(dest);

                        if (sudo) {
                            command.useSudo();
                        }

                        SshCommand sshCommand = command.withSsh(sshConfig);
                        sshCommand.run();
                    }
                }
            } finally {
                if (tempFile != null) {
                    tempFile.close();
                }
            }
        }
    }

}
