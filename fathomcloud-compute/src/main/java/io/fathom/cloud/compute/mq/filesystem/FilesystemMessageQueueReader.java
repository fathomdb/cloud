package io.fathom.cloud.compute.mq.filesystem;

import io.fathom.cloud.mq.MessageQueueReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class FilesystemMessageQueueReader implements MessageQueueReader {
    private static final Logger log = LoggerFactory.getLogger(FilesystemMessageQueueReader.class);

    private final File queueDir;

    public FilesystemMessageQueueReader(File queueDir) throws IOException {
        this.queueDir = queueDir;

    }

    @Override
    public byte[] poll() throws IOException {
        String[] names = queueDir.list();
        if (names.length == 0) {
            return null;
        }

        Arrays.sort(names);

        for (String name : names) {
            char firstChar = name.charAt(0);
            if (firstChar == '.' || firstChar == '_') {
                continue;
            }
            File file = new File(queueDir, name);
            byte[] data = Files.toByteArray(file);
            if (!file.delete()) {
                throw new IOException("Unable to delete file: " + file);
            }
            return data;
        }

        return null;
    }

}
