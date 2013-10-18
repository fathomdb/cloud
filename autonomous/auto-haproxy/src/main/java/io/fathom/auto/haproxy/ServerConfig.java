package io.fathom.auto.haproxy;

import io.fathom.auto.config.ConfigPath;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class ServerConfig extends ConfigSyncBase {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    final File mirrorPath;

    private boolean dirty;

    public ServerConfig(ConfigPath base, File mirrorPath) {
        super(base);

        this.mirrorPath = mirrorPath;
    }

    @Override
    protected boolean updateRecord(Record record) throws SQLException, IOException {
        String json = base.readChild(record.name);
        if (json == null) {
            log.info("Zone file no longer exists: {}", record.name);
            // TODO: Delete file??
            return false;
        }

        File file = new File(mirrorPath, escape(record.name));

        Files.write(json.getBytes(Charsets.UTF_8), file);

        dirty = true;

        return true;

    }

    @Override
    protected boolean deleteRecord(Record record) throws SQLException, IOException {
        File file = new File(mirrorPath, escape(record.name));

        if (!file.delete()) {
            log.error("Unable to delete mirror file: {}", file);
            return false;
        }

        dirty = true;

        return true;
    }

    public static String escape(String s) {
        String escaped;
        try {
            escaped = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException();
        }
        // escaped = escaped.replace('%', '_');
        return escaped;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }
}
