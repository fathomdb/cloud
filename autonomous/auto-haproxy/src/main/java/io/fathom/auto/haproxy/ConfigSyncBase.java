package io.fathom.auto.haproxy;

import io.fathom.auto.config.ConfigEntry;
import io.fathom.auto.config.ConfigPath;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class ConfigSyncBase implements ConfigSync {
    private static final Logger log = LoggerFactory.getLogger(ConfigSyncBase.class);

    // TODO: Abstract out
    final ConfigPath base;

    final Map<String, Record> records;

    public ConfigSyncBase(ConfigPath base) {
        this.base = base;
        this.records = Maps.newHashMap();
    }

    public static class Record {
        final String name;
        long lastModified;

        public Record(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }
    }

    public synchronized int refresh() throws IOException {
        Set<String> found = Sets.newHashSet();

        List<Record> changed = Lists.newArrayList();

        // TODO: Make multithreaded??

        for (ConfigEntry o : base.listChildren()) {
            long time = o.getVersion();

            String name = o.getName();

            Record record = records.get(name);
            if (record == null) {
                log.debug("Adding record: {}", name);
                record = new Record(name, time);
                changed.add(record);
                records.put(name, new Record(name, time));
            } else {
                found.add(record.name);

                if (record.lastModified != time) {
                    log.debug("Updating record: {}", name);
                    changed.add(record);
                }
            }
        }

        List<Record> delete = Lists.newArrayList();
        for (Record record : records.values()) {
            if (!found.contains(record.name)) {
                log.debug("Deleting zone: {}", record.name);
                delete.add(record);
                records.remove(record.name);
            }
        }

        return synchronizeChanges(changed, delete);
    }

    public synchronized int firstSync() throws IOException {
        int failures = 0;

        // TODO: Make multithreaded??

        for (ConfigEntry o : base.listChildren()) {
            long time = o.getVersion();

            String name = o.getName();

            try {
                Record record = new Record(name, time);

                if (updateRecord(record)) {
                    records.put(name, record);
                } else {
                    // This isn't actually a failure; it means the record didn't
                    // exist when we went to read it
                    failures++;
                }
            } catch (Exception e) {
                log.error("Error updating record: " + name, e);
                failures++;
            }
        }

        return failures;
    }

    protected int synchronizeChanges(Collection<Record> upsert, Collection<Record> delete) {
        int failures = 0;

        // TODO: Multithreaded?
        for (Record record : upsert) {
            try {
                updateRecord(record);
            } catch (Exception e) {
                log.error("Error updating record: " + record.name, e);
                failures++;
            }
        }

        for (Record record : delete) {
            try {
                deleteRecord(record);
            } catch (Exception e) {
                log.error("Error inserting record: " + record.name, e);
                failures++;
            }
        }
        return failures;
    }

    protected abstract boolean updateRecord(Record record) throws SQLException, IOException;

    protected abstract boolean deleteRecord(Record record) throws SQLException, IOException;
}
