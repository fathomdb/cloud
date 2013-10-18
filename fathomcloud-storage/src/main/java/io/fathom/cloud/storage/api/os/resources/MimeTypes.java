package io.fathom.cloud.storage.api.os.resources;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class MimeTypes {
    public static final MimeTypes INSTANCE;

    static {
        File path = new File("/etc/mime.types");
        try {
            INSTANCE = new MimeTypes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + path);
        }

    }

    private MimeTypes(File path) throws IOException {
        parse(path);
    }

    final Map<String, String> extensionToMimeType = Maps.newHashMap();

    private void parse(File path) throws IOException {
        Files.readLines(path, Charsets.UTF_8, new LineProcessor<Integer>() {

            int count = 0;

            @Override
            public boolean processLine(String line) throws IOException {
                line = line.trim();
                if (line.isEmpty()) {
                    return true;
                }

                if (line.startsWith("#")) {
                    return true;
                }

                String mimeType = null;
                for (String token : Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(line)) {
                    if (mimeType == null) {
                        mimeType = token;
                    } else {
                        extensionToMimeType.put(token, mimeType);
                    }
                }
                return true;
            }

            @Override
            public Integer getResult() {
                return count;
            }
        });
    }

    public String guessMimeType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        }

        String extension = path.substring(lastDot + 1);
        extension = extension.toLowerCase();

        String mimeType = extensionToMimeType.get(extension);
        return mimeType;
    }
}
