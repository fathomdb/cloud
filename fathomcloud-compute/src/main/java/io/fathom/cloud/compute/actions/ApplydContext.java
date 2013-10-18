package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.SshCommand;
import io.fathom.cloud.sftp.RemoteFile;
import io.fathom.cloud.sftp.Sftp;
import io.fathom.cloud.ssh.SftpStat;
import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.io.ByteStreams;

public class ApplydContext {
    private static final Logger log = LoggerFactory.getLogger(ApplydContext.class);

    final Sftp sftp;

    public ApplydContext(Sftp sftp) {
        this.sftp = sftp;
    }

    public boolean updateConfig(String path, String data) throws CloudException {
        return updateConfig(path, data.getBytes(Charsets.UTF_8));
    }

    public boolean updateConfig(String path, byte[] data) throws CloudException {
        File base = new File("/etc/apply.d/");
        File confFile = new File(base, path);

        log.debug("SFTP writing config file {}", confFile);

        try {
            byte[] src = sftp.readAllBytes(confFile);
            if (src != null) {
                if (Arrays.equals(src, data)) {
                    log.debug("SFTP config file unchanged; won't write: {}", confFile);
                    return false;
                }
            }

            int mode = 0660;
            sftp.writeAtomic(new RemoteFile(confFile), ByteStreams.asByteSource(data), mode);
        } catch (IOException e) {
            throw new CloudException("Error updating applyd configuration", e);
        }
        return true;
    }

    public boolean removeConfig(String path) throws CloudException {
        File base = new File("/etc/apply.d/");
        File confFile = new File(base, path);

        log.debug("SFTP deleting config file {}", confFile);

        try {
            SftpStat stat = sftp.stat(confFile);
            if (stat == null) {
                return false;
            }

            sftp.delete(confFile);
        } catch (IOException e) {
            throw new CloudException("Error updating applyd configuration", e);
        }
        return true;
    }

    public void apply(SshConfig sshConfig) throws CloudException {
        String cmd;
        if (Objects.equal("root", sshConfig.getUser())) {
            cmd = "/usr/sbin/applyd";
        } else {
            // TODO: Make suid?
            cmd = "sudo /usr/sbin/applyd";
        }

        try {
            SshCommand sshCommand = new SshCommand(sshConfig, cmd);
            sshCommand.run();
        } catch (IOException e) {
            throw new CloudException("Error applying applyd configuration", e);
        }
    }

}
