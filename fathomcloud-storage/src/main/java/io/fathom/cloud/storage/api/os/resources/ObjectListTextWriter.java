package io.fathom.cloud.storage.api.os.resources;

import java.io.IOException;

public class ObjectListTextWriter extends TextWriter {

    final Iterable<DirectoryListEntry> entries;

    public ObjectListTextWriter(Iterable<DirectoryListEntry> entries) {
        this.entries = entries;
    }

    @Override
    protected void write0() throws IOException {
        int count = 0;

        for (DirectoryListEntry entry : entries) {
            if (count != 0) {
                output.write('\n');
            }
            count++;

            output.write(entry.getKey());
        }
    }

}
