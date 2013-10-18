package io.fathom.cloud.sftp;

import java.io.File;

public class RemoteFile {
    private final File file;

    public RemoteFile(File file) {
        super();
        this.file = file;
    }

    public RemoteFile(RemoteFile parent, String child) {
        this(new File(parent.file, child));
    }

    public RemoteFile getParentFile() {
        File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }
        return new RemoteFile(parent);
    }

    public File getSshPath() {
        return file;
    }

    @Override
    public String toString() {
        return file.toString();
    }

}
