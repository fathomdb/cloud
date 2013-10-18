package io.fathom.cloud.dns.backend;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.model.DnsRecord;
import io.fathom.cloud.dns.model.DnsRecordset;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.dns.state.DnsRepository;
import io.fathom.cloud.openstack.client.dns.model.Record;
import io.fathom.cloud.openstack.client.dns.model.Recordset;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.StoreOptions;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class DnsBackendBase implements DnsBackend {
    private static final Logger log = LoggerFactory.getLogger(DnsBackendBase.class);

    @Inject
    protected DnsRepository repository;

    @Inject
    protected DnsServiceImpl dns;

    public abstract class UpdateDnsDomainBase implements Callable<Void> {
        protected final Project project;
        protected final DnsZone zone;

        public UpdateDnsDomainBase(Project project, DnsZone zone) {
            this.project = project;
            this.zone = zone;
        }

        public class Changes {
            public List<Recordset> remove = Lists.newArrayList();
            public List<Recordset> create = Lists.newArrayList();
        }

        protected Changes computeChanges(List<Recordset> current, List<Recordset> desired) {
            Multimap<String, Recordset> currentMap = HashMultimap.create();
            for (Recordset currentRecordset : current) {
                String key = currentRecordset.type + ":" + currentRecordset.name;
                currentMap.put(key, currentRecordset);
            }

            Changes changes = new Changes();

            Set<String> managedNames = Sets.newConcurrentHashSet();

            Multimap<String, Recordset> matches = HashMultimap.create();
            for (Recordset desiredRecordset : desired) {
                String key = desiredRecordset.type + ":" + desiredRecordset.name;

                managedNames.add(key);

                Recordset found = null;
                for (Recordset rrs : currentMap.get(key)) {
                    if (!rrs.matches(desiredRecordset)) {
                        continue;
                    }

                    found = rrs;
                    break;
                }

                if (found == null) {
                    // Not found; need to create

                    log.debug("Creating record: {}", desiredRecordset);

                    if (!desiredRecordset.isDeleted()) {
                        changes.create.add(desiredRecordset);
                    }
                } else {
                    log.debug("Matched existing record: {}", found);

                    matches.put(key, found);
                }
            }

            for (Entry<String, Recordset> entry : currentMap.entries()) {
                String key = entry.getKey();

                if (!managedNames.contains(key)) {
                    // Note: we rely on state to keep around deleted records
                    // (currently forever!)
                    // Also, we can't do record rename / retype
                    log.debug("Record not managed by this domain: {}", key);
                    continue;
                }

                Recordset rrs = entry.getValue();

                boolean found = false;

                for (Recordset i : matches.get(entry.getKey())) {
                    if (rrs.matches(i)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    log.debug("Deleting record: {}", rrs);
                    changes.remove.add(rrs);
                } else {
                    log.debug("Found matching record: {}", rrs);
                }
            }

            return changes;
        }

        protected List<Recordset> readFromDatabase(boolean createSoa) throws CloudException {
            String zoneName = zone.getName();
            List<DnsRecordset> records = dns.listRecordsets(project, zone, StoreOptions.ShowDeleted);

            List<Recordset> recordsets = Lists.newArrayList();

            boolean hasSoa = false;

            Set<String> ids = Sets.newHashSet();

            for (DnsRecordset recordset : records) {
                DnsRecordsetData data = recordset.getData();

                Recordset r = new Recordset();

                String fqdn = recordset.getFqdn();

                r.name = fqdn;
                r.id = "" + data.getId();
                if (ids.contains(r.id)) {
                    throw new IllegalStateException();
                }
                ids.add(r.id);

                r.zone_id = "" + data.getZoneId();

                r.weight = null;
                r.ttl = null;
                r.type = data.getType();

                if (data.hasState()) {
                    r.deleted_at = Clock.toDate(data.getState().getDeletedAt());
                }

                if (DnsService.TYPE_SOA.equalsIgnoreCase(r.type)) {
                    hasSoa = true;
                }

                r.records = Lists.newArrayList();
                for (DnsRecord dnsRecord : recordset.getRecords()) {
                    Record record = new Record();
                    DnsRecordData recordData = dnsRecord.getData();
                    record.value = recordData.getTarget();
                    if (recordData.hasWeight()) {
                        record.weight = recordData.getWeight();
                    }
                    if (recordData.hasPort()) {
                        record.port = recordData.getPort();
                    }
                    if (recordData.hasPriority()) {
                        record.priority = recordData.getPriority();
                    }
                    r.records.add(record);
                }

                recordsets.add(r);
            }

            if (createSoa && !hasSoa) {
                Recordset r = new Recordset();
                r.name = zoneName;

                // primary hostmaster serial refresh retry expire default_ttl
                String primary = "ns." + zoneName;
                String hostmaster = "hostmaster@" + zoneName;
                String serial = Long.toString(System.currentTimeMillis() / 1000L);

                int refresh = 7200;
                int retry = 900;
                int expire = 1209600;
                int defaultTtl = 86400;

                r.records = Lists.newArrayList();
                {
                    Record record = new Record();
                    record.value = primary + " " + hostmaster + " " + serial + " " + refresh + " " + retry + " "
                            + expire + " " + defaultTtl;
                    r.records.add(record);
                }

                int id = 1;
                while (ids.contains(Integer.toString(id))) {
                    id++;
                }
                r.id = Integer.toString(id);
                ids.add(r.id);
                r.zone_id = "" + zone.getId();

                r.weight = null;
                r.ttl = null;
                r.type = DnsService.TYPE_SOA;

                log.info("Adding synthetic SOA record: {}", r);

                recordsets.add(r);
            }

            return recordsets;
        }
    }

}
