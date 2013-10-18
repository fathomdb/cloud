package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.protobuf.FileModel.FileData;

import java.io.IOException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fathomdb.utils.Hex;

public class ObjectListJsonWriter extends JsonWriter {

    final Iterable<DirectoryListEntry> entries;

    public ObjectListJsonWriter(Iterable<DirectoryListEntry> entries) {
        this.entries = entries;
    }

    @Override
    protected void write0() throws IOException {
        startArray();

        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

        int count = 0;

        for (DirectoryListEntry entry : entries) {
            if (count != 0) {
                writeComma();
            }
            count++;

            startObject();

            if (entry.isDirectory()) {
                writeKeyLiteral("subdir");
                writeValue(entry.getKey());

                // writeComma();
                // writeKeyLiteral("content_type");
                // writeValue("application/directory");
            } else {
                FileData file = entry.getFile();

                writeKeyLiteral("name");
                writeValue(file.getKey());

                if (file.hasHash()) {
                    writeComma();
                    writeKeyLiteral("hash");
                    writeValue(Hex.toHex(file.getHash().toByteArray()));
                }

                if (file.hasLength()) {
                    writeComma();
                    writeKeyLiteral("bytes");
                    writeValue(Long.toString(file.getLength()));
                }

                if (file.hasContentType()) {
                    writeComma();
                    writeKeyLiteral("content_type");
                    writeValue(file.getContentType());
                }

                if (file.hasLastModified()) {
                    writeComma();
                    writeKeyLiteral("last_modified");
                    writeValue(formatter.print(file.getLastModified()));
                }
            }
            endObject();
        }

        endArray();
    }

}
