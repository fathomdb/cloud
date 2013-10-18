package io.fathom.cloud.compute.mq.filesystem;

import io.fathom.cloud.blobs.TempFile;
import io.fathom.cloud.mq.MessageQueueWriter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.io.IoUtils;
import com.google.common.io.Files;

public class FilesystemMessageQueueWriter implements MessageQueueWriter {
    private static final Logger log = LoggerFactory.getLogger(FilesystemMessageQueueWriter.class);

    private final File queueDir;

    private final File tmpDir;

    public FilesystemMessageQueueWriter(File queueDir) throws IOException {
        this.queueDir = queueDir;

        this.tmpDir = new File(queueDir, "_tmp");
        IoUtils.mkdirs(tmpDir);
    }

    @Override
    public void enqueue(byte[] data) throws IOException {
        String name = System.currentTimeMillis() + "_" + UUID.randomUUID().toString();

        try (TempFile tempFile = new TempFile(new File(tmpDir, name))) {
            Files.write(data, tempFile.getFile());

            tempFile.renameTo(new File(queueDir, name));
        }
    }

}
