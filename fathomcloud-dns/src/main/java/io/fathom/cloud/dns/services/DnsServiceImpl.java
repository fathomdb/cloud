package io.fathom.cloud.dns.services;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.backend.DnsBackend;
import io.fathom.cloud.dns.model.DnsRecordset;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.state.DnsRepository;
import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.StoreOptions;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.net.InetAddresses;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class DnsServiceImpl implements DnsService, LifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(DnsServiceImpl.class);

    @Inject
    DnsRepository repository;

    @Inject
    WellKnownTlds wellKnownTlds;

    @Inject
    DnsBackends dnsBackends;

    private final boolean canCreateInTld;

    @Inject
    public DnsServiceImpl(Configuration config) {
        // TODO: Add @Configured?
        this.canCreateInTld = config.lookup("dns.allowCreateInTld", true);
    }

    @Override
    public List<DnsZone> listZones(Project project) throws CloudException {
        List<DnsZone> records = Lists.newArrayList();
        for (DnsZoneData data : repository.getDnsZones(project.getId()).list()) {
            DnsZone record = new DnsZone(data);
            records.add(record);
        }
        return records;
    }

    @Override
    public DnsZone createZone(Project project, DnsZoneSpec spec) throws CloudException, DuplicateValueException {
        String zoneName = spec.name;
        zoneName = zoneName.toLowerCase();

        DnsSuffixData suffixData = findMaximalSuffix(zoneName);
        if (suffixData == null) {
            throw new IllegalArgumentException("Unsupported suffix: " + zoneName);
        }

        if (suffixData.getTld() && !canCreateInTld) {
            throw new IllegalArgumentException("Creation of domains blocked (under TLDs)");
        }

        String backendKey = null;
        String zone;

        // TODO: Is there any point in allowing a shared domain but then
        // blocking creation under it?
        // if (suffixData.getSharedDomain() && !canCreateInShared) {
        // throw new
        // IllegalArgumentException("Creation of domains blocked (under shared domains)");
        // }

        String suffix = suffixData.getKey();
        String prefix = zoneName.substring(0, zoneName.length() - suffix.length());
        prefix = CharMatcher.is('.').trimTrailingFrom(prefix);

        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("Unsupported domain (TLD or marked as shared): " + zoneName);
        } else {
            int dotIndex = prefix.indexOf('.');
            String prefixTail;
            if (dotIndex != -1) {
                prefixTail = prefix.substring(dotIndex + 1);
            } else {
                prefixTail = prefix;
            }

            // If this is a shared domain, try to assign it to the user
            if (suffixData.getSharedDomain()) {
                NamedItemCollection<DnsSuffixData> store = repository.getSharedSubdomains(suffixData.getKey());
                DnsSuffixData allocation = store.find(prefixTail);
                if (allocation == null) {
                    DnsSuffixData.Builder b = DnsSuffixData.newBuilder();
                    b.setKey(prefixTail);
                    b.setOwnerProject(project.getId());

                    allocation = store.create(b);
                } else {
                    if (allocation.getOwnerProject() != project.getId()) {
                        // Already assigned to someone else
                        throw new DuplicateValueException();
                    }
                }
            }

            if (suffixData.hasBackend()) {
                backendKey = suffixData.getBackend();
            }

            zone = prefixTail + "." + suffix;
        }

        if (spec.backend != null) {
            if (backendKey != null && !backendKey.equals(spec.backend)) {
                throw new IllegalArgumentException();
            }
        }

        if (backendKey == null) {
            if (spec.backend != null) {
                backendKey = spec.backend;
            } else {
                // TODO: Cache ?
                for (BackendData backend : repository.getBackends().list()) {
                    if (backend.getDefault()) {
                        backendKey = backend.getKey();
                    }
                }
            }
        }

        DnsBackend backend = getBackend(backendKey);

        String backendId = backend.createZone(project, zoneName, zone, suffixData);

        DnsZoneData.Builder data = DnsZoneData.newBuilder();
        data.setProjectId(project.getId());
        data.setName(zoneName);
        if (backendKey != null) {
            data.setBackend(backendKey);
        }
        if (backendId != null) {
            data.setBackendId(backendId);
        }

        DnsZoneData created = repository.getDnsZones(project.getId()).create(data);

        DnsZone dnsDomain = new DnsZone(created);
        backend.updateDomain(project, dnsDomain);
        return dnsDomain;
    }

    public DnsSuffixData findMaximalSuffix(String domain) throws CloudException {
        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();

        domain = domain.toLowerCase();

        while (true) {
            if (Strings.isNullOrEmpty(domain)) {
                return null;
            }

            DnsSuffixData data = store.find(domain);
            if (data != null) {
                return data;
            }

            int dotIndex = domain.indexOf('.');
            domain = domain.substring(dotIndex + 1);
        }
    }

    private boolean isTld(String domain) throws CloudException {
        // TODO: Caching

        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();
        domain = domain.toLowerCase();

        DnsSuffixData data = store.find(domain);
        if (data != null) {
            return data.hasTld() && data.getTld();
        }

        return false;

        // tlds.add("com");
        // tlds.add("net");
        // tlds.add("org");
        // tlds.add("edu");
        // tlds.add("gov");
        // tlds.add("mil");
        // tlds.add("io");
        // tlds.add("co.uk");
        //
        // domain = domain.toLowerCase();
        // if (tlds.contains(domain)) {
        // return true;
        // }
        //
        // boolean found = false;
        // for (String tld : tlds) {
        // if (domain.endsWith("." + tld)) {
        // found = true;
        // break;
        // }
        // }
        //
        // if (!found) {
        // log.warn("Did not recognize TLD for: {}", domain);
        // }
        // return false;
    }

    // public String findHost(String fqdn, String domain) {
    // if (!fqdn.endsWith(domain)) {
    // throw new IllegalArgumentException();
    // }
    //
    // String host = fqdn.substring(0, fqdn.length() - domain.length());
    // // if (host.isEmpty()) {
    // // host = ".";
    // // }
    //
    // if (host.endsWith(".")) {
    // host = host.substring(0, host.length() - 1);
    // }
    // return host;
    // }

    @Override
    public DnsZone findZoneByName(Project project, String zoneName) throws CloudException {
        NumberedItemCollection<DnsZoneData> dnsDomains = repository.getDnsZones(project.getId());
        DnsZoneData data = dnsDomains.findByKey(zoneName);
        if (data == null) {
            return null;
        }
        return new DnsZone(data);
    }

    @Override
    public DnsZone findDomain(Project project, long id) throws CloudException {
        DnsZoneData data = repository.getDnsZones(project.getId()).find(id);
        if (data == null) {
            return null;
        }
        return new DnsZone(data);
    }

    @Override
    public Recordset findRecordset(Project project, Zone zone, long recordsetId) throws CloudException {
        DnsRecordsetData data = repository.getDnsRecordsets(project.getId(), zone.getId()).find(recordsetId);
        if (data == null) {
            return null;
        }
        return new DnsRecordset((DnsZone) zone, data);
    }

    // public DnsRecord findRecord(Project project, DnsDomain domain, String
    // name) throws CloudException {
    // long projectId = project.getId();
    // if (domain.getProjectId() != projectId) {
    // throw new IllegalArgumentException();
    // }
    //
    // DnsRecordData data = repository.getDnsRecords(projectId,
    // domain.getName()).find(name);
    // if (data == null) {
    // return null;
    // }
    // return new DnsRecord(domain, data);
    // }

    public DnsRecordset createRecordset(Project project, DnsZone zone, DnsRecordsetData.Builder data)
            throws CloudException {
        long projectId = project.getId();
        if (zone.getProjectId() != projectId) {
            throw new IllegalArgumentException();
        }

        data.setProjectId(projectId);

        DnsBackend backend = findBackend(zone);

        data.getStateBuilder().setCreatedAt(Clock.getTimestamp());

        DnsRecordsetData created = repository.getDnsRecordsets(projectId, zone.getId()).create(data);
        backend.updateDomain(project, zone);
        return new DnsRecordset(zone, created);
    }

    private DnsBackend findBackend(DnsZone zone) throws CloudException {
        String backendKey = null;
        if (zone.getData().hasBackend()) {
            backendKey = zone.getData().getBackend();
        }
        return getBackend(backendKey);
    }

    private DnsBackend getBackend(String backendKey) throws CloudException {
        if (backendKey == null) {
            return dnsBackends.getGenericBackend();
        }

        BackendData backend = repository.getBackends().find(backendKey);
        if (backend == null) {
            throw new IllegalArgumentException("Cannot find backend");
        }
        return dnsBackends.getBackend(backend);
    }

    String findDomain(String host) throws CloudException {
        int firstDot = host.indexOf('.');
        if (firstDot == -1) {
            log.warn("Suspicious domain; likely won't validate: " + host);
            return host;
        }

        // String prefix = host.substring(0, firstDot);
        String suffix = host.substring(firstDot + 1);
        if (isTld(suffix)) {
            return host;
        }

        return suffix;
    }

    // static class RecordImpl implements DnsService.Record {
    // private final String type;
    // private final String fqdn;
    // private final InetAddress ip;
    //
    // public RecordImpl(String type, String fqdn, InetAddress ip) {
    // this.type = type;
    // this.fqdn = fqdn;
    // this.ip = ip;
    // }
    //
    // }

    // @Override
    // public Record buildAddress(String fqdn, InetAddress ip) {
    // String type = (ip instanceof Inet6Address ? DnsService.TYPE_AAAA :
    // DnsService.TYPE_A);
    // return new RecordImpl(type, fqdn, ip);
    // }

    @Override
    public void setDnsRecordsets(String systemKey, Project project, List<DnsRecordsetSpec> recordsetSpecs)
            throws CloudException {
        List<DnsRecordsetData> recordsets = Lists.newArrayList();

        for (DnsRecordsetSpec recordsetSpec : recordsetSpecs) {
            // DnsRecord recordImpl = (DnsRecord) record;

            String domainName = findDomain(recordsetSpec.fqdn);
            // String name = findHost(recordset.fqdn, domainName);

            DnsZoneSpec zoneSpec = new DnsZoneSpec();
            zoneSpec.name = domainName;

            DnsZone zone = findOrCreateDomain(project, zoneSpec);

            DnsRecordsetData.Builder data = DnsRecordsetData.newBuilder();
            data.setProjectId(project.getId());

            for (DnsRecordSpec record : recordsetSpec.records) {
                // Sanity check
                InetAddress address = InetAddresses.forString(record.address);
                String ip = InetAddresses.toAddrString(address);

                DnsRecordData.Builder recordBuilder = data.addRecordBuilder();
                recordBuilder.setTarget(ip);
            }

            data.setType(recordsetSpec.type);
            data.setFqdn(recordsetSpec.fqdn);
            data.setZoneId(zone.getId());
            data.setSystemKey(systemKey);

            recordsets.add(data.build());
        }

        setDnsRecords(project, recordsets);
    }

    private void setDnsRecords(Project project, List<DnsRecordsetData> recordsets) throws CloudException {
        // This is where a database would be awesome!

        long projectId = project.getId();

        Set<Long> idsInUse = Sets.newHashSet();

        // TODO: This is inefficient
        Multimap<String, DnsRecordsetData> dbState = HashMultimap.create();
        for (DnsZoneData domain : repository.getDnsZones(projectId).list()) {
            for (DnsRecordsetData record : repository.getDnsRecordsets(projectId, domain.getId()).list()) {
                idsInUse.add(record.getId());

                if (!record.hasSystemKey()) {
                    continue;
                }
                dbState.put(record.getSystemKey(), record);
            }
        }

        ImmutableListMultimap<String, DnsRecordsetData> requestedState = Multimaps.index(recordsets,
                new Function<DnsRecordsetData, String>() {
                    @Override
                    public String apply(DnsRecordsetData input) {
                        return input.getSystemKey();
                    }
                });

        Set<Long> dirtyDomains = Sets.newHashSet();

        for (String systemKey : requestedState.keys()) {
            Set<DnsRecordsetData> requested = Sets.newHashSet(requestedState.get(systemKey));
            Set<DnsRecordsetData> current = Sets.newHashSet(dbState.get(systemKey));

            SetView<DnsRecordsetData> add = Sets.difference(requested, current);
            SetView<DnsRecordsetData> remove = Sets.difference(current, requested);

            for (DnsRecordsetData a : add) {
                DnsRecordsetData.Builder b = DnsRecordsetData.newBuilder(a);
                repository.getDnsRecordsets(projectId, a.getZoneId()).create(b);

                dirtyDomains.add(b.getZoneId());
            }

            for (DnsRecordsetData r : remove) {
                repository.getDnsRecordsets(projectId, r.getZoneId()).delete(r.getId());
                dirtyDomains.add(r.getZoneId());
            }
        }

        for (Long domainId : dirtyDomains) {
            DnsZone zone = findDomain(project, domainId);

            DnsBackend backend = findBackend(zone);

            backend.updateDomain(project, zone);
        }
    }

    private DnsZone findOrCreateDomain(Project project, DnsZoneSpec zoneSpec) throws CloudException {
        DnsZone zone = findZoneByName(project, zoneSpec.name);

        if (zone == null) {
            try {
                zone = createZone(project, zoneSpec);
            } catch (DuplicateValueException e) {
                zone = findZoneByName(project, zoneSpec.name);
            }

            if (zone == null) {
                throw new IllegalStateException();
            }
        }

        return zone;
    }

    @Override
    public List<DnsRecordset> listRecordsets(Project project, Zone zone) throws CloudException {
        return listRecordsets(project, zone, null);
    }

    public List<DnsRecordset> listRecordsets(Project project, Zone zone, StoreOptions... options) throws CloudException {
        long projectId = project.getId();
        if (zone.getProjectId() != projectId) {
            throw new IllegalArgumentException();
        }
        List<DnsRecordset> records = Lists.newArrayList();
        for (DnsRecordsetData data : repository.getDnsRecordsets(projectId, zone.getId()).list(options)) {
            DnsRecordset record = new DnsRecordset((DnsZone) zone, data);
            records.add(record);
        }
        return records;
    }

    @Override
    public DnsRecordset createRecordset(Project project, Zone zone, String fqdn, String type, List<String> ips)
            throws CloudException {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (Strings.isNullOrEmpty(type)) {
            throw new IllegalArgumentException();
        }

        type = type.trim().toUpperCase();
        if (Strings.isNullOrEmpty(type)) {
            throw new IllegalArgumentException();
        }

        DnsRecordsetData.Builder data = DnsRecordsetData.newBuilder();
        data.setZoneId(zone.getId());
        data.setFqdn(fqdn);
        data.setType(type);

        for (String ip : ips) {
            // Sanity check
            InetAddress address = InetAddresses.forString(ip);
            ip = InetAddresses.toAddrString(address);

            DnsRecordData.Builder recordBuilder = data.addRecordBuilder();
            recordBuilder.setTarget(ip);
        }

        data.setProjectId(project.getId());

        return createRecordset(project, (DnsZone) zone, data);
    }

    private DnsRecordset toRecord(DnsZone zone, DnsRecordsetData data) {
        if (data == null) {
            return null;
        }
        return new DnsRecordset(zone, data);
    }

    @Override
    public void start() throws Exception {
        wellKnownTlds.create();
    }

    public List<DnsSuffixData> listSuffix() throws CloudException {
        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();
        return store.list();
    }

    public DnsSuffixData ensureTld(String tld) throws CloudException, DuplicateValueException {
        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();

        tld = tld.trim();
        while (tld.startsWith(".")) {
            tld = tld.substring(1);
        }
        tld = tld.toLowerCase();

        DnsSuffixData data = store.find(tld);
        if (data != null) {
            if (!data.getTld()) {
                throw new IllegalArgumentException("Suffix is not marked as TLD: " + tld);
            }
            return data;
        }

        DnsSuffixData.Builder b = DnsSuffixData.newBuilder();
        b.setKey(tld);
        b.setTld(true);
        return store.create(b);
    }

    public DnsSuffixData createShared(Zone zone) throws CloudException, DuplicateValueException {
        DnsZoneData data = ((DnsZone) zone).getData();

        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();
        String key = zone.getName();
        key = key.toLowerCase();

        DnsSuffixData.Builder b = DnsSuffixData.newBuilder();
        b.setKey(key);
        b.setSharedDomain(true);
        b.setOwnerProject(zone.getProjectId());
        if (data.hasBackend()) {
            b.setBackend(data.getBackend());
        }
        return store.create(b);
    }

    public DnsSuffixData deleteTld(String tld) throws CloudException {
        NamedItemCollection<DnsSuffixData> store = repository.getDnsSuffixes();
        DnsSuffixData ret = store.find(tld);

        store.delete(tld);

        return ret;
    }

    public boolean deleteZone(Project project, DnsZone zone) throws CloudException {
        // TODO: Verify no children
        // TODO: Delete from backend?

        NumberedItemCollection<DnsZoneData> store = repository.getDnsZones(project.getId());
        DnsZoneData found = store.find(zone.getId());
        if (found == null) {
            return false;
        }
        store.delete(zone.getId());
        return true;
    }

    public boolean deleteRecordset(Project project, DnsZone zone, long recordsetId) throws CloudException {
        NumberedItemCollection<DnsRecordsetData> store = repository.getDnsRecordsets(project.getId(), zone.getId());

        DnsRecordsetData found = store.find(recordsetId);
        if (found == null) {
            return false;
        }

        DnsBackend backend = findBackend(zone);

        store.delete(recordsetId);

        backend.updateDomain(project, zone);

        return true;
    }

    public DnsZone findMaximalZone(Project project, String name) throws CloudException {
        NumberedItemCollection<DnsZoneData> store = repository.getDnsZones(project.getId());

        name = name.toLowerCase();

        while (true) {
            if (Strings.isNullOrEmpty(name)) {
                return null;
            }

            DnsZoneData data = store.findByKey(name);
            if (data != null) {
                return new DnsZone(data);
            }

            int dotIndex = name.indexOf('.');
            if (dotIndex == -1) {
                return null;
            }

            name = name.substring(dotIndex + 1);
        }
    }

}
