package io.fathom.cloud.ssh.jsch;

import io.fathom.cloud.ssh.SftpStat;

import com.jcraft.jsch.SftpATTRS;

public class JschSftpStat implements SftpStat {

    private final SftpATTRS attrs;

    public JschSftpStat(SftpATTRS attrs) {
        this.attrs = attrs;
    }

    @Override
    public long size() {
        return attrs.getSize();
    }

}
