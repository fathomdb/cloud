package io.fathom.cloud.storage.services;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

public class MimeTypes {
    public static final MimeTypes INSTANCE;

    static {
        try {
            URL resource = Resources.getResource(MimeTypes.class, "/mime.types");
            INSTANCE = new MimeTypes(Resources.asCharSource(resource, Charsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read mime.types resource", e);
        }
    }

    private MimeTypes(CharSource source) throws IOException {
        parse(source);
    }

    final Map<String, String> extensionToMimeType = Maps.newHashMap();

    private void parse(CharSource source) throws IOException {
        CharStreams.readLines(source, new LineProcessor<Integer>() {

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
